package diary.diaryai.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.diaryai.entity.ai.dto.AiTaskMessageDto;
import diary.diaryai.entity.ai.dto.AiTaskProcessDto;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.enums.aienum.AIEnum;
import diary.diaryai.enums.aienum.AiTaskErrorCodeEnum;
import diary.diaryai.enums.aienum.AiTaskStatusEnum;
import diary.diaryai.enums.aienum.AiTaskTypeEnum;
import diary.diaryai.executor.AiTaskExecutor;
import diary.diaryai.executor.AiTaskModelResolver;
import diary.diaryai.guard.AiTaskLeaseRenewer;
import diary.diaryai.guard.LocalAiConcurrencyGuard;
import diary.diaryai.guard.RedisModelPermitRenewer;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.redis.RedisModelPermitManager;
import diary.diaryai.service.AiTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.annotation.RocketMQMessageListener;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.core.RocketMQListener;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

import static diary.diaryai.consts.MqTaskConst.OUTBOX_SCHEMA_VERSION;

@Service
@Slf4j
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = "${diary.ai.rocketmq.ai-task-consumer-group:diary-ai-task-workers}",
        topic = "${diary.ai.rocketmq.task-topic:diary-ai-task}",
        tag = "${diary.ai.rocketmq.ai-task-consumer-tag:NUTRIENT_ANALYSE || SENTIMENT_ANALYSIS}",
        sslEnabled = false
)
public class AiOutboxConsumer implements RocketMQListener {
    private final AiTaskProperties properties;
    private final ObjectMapper objectMapper;
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskExecutor aiTaskExecutor;
    private final AiTaskModelResolver modelResolver;
    private final LocalAiConcurrencyGuard localConcurrencyGuard;
    private final RedisModelPermitManager globalPermitManager;
    private final RedisModelPermitRenewer globalPermitRenewer;
    private final AiTaskLeaseRenewer taskLeaseRenewer;
    private final AiTransactionService transactionService;

    @Override
    public ConsumeResult consume(MessageView messageView) {
        AiTaskMessageDto message = parseMessage(messageView);
        if (message == null) return ConsumeResult.SUCCESS;

        try {
            AiTaskPO task = diaryAiMapper.selectAiTaskByTaskId(message.getTaskId());
            if (task == null) {
                log.error("AI任务不存在，忽略永久无效消息, taskId={}", message.getTaskId());
                return ConsumeResult.SUCCESS;
            }
            if (isTerminal(task.getStatus())) return ConsumeResult.SUCCESS;
            if (task.getAttemptCount() >= task.getMaxAttempts()) {
                transactionService.failExhaustedTask(task, "任务执行次数已耗尽");
                return ConsumeResult.SUCCESS;
            }
            return executeTask(message, task);
        } catch (RuntimeException exception) {
            log.error("AI任务消费失败，等待RocketMQ重投, taskId={}", message.getTaskId(), exception);
            return ConsumeResult.FAILURE;
        }
    }

    private ConsumeResult executeTask(AiTaskMessageDto message, AiTaskPO task) {
        AIEnum model = modelResolver.resolve(task);
        if (!localConcurrencyGuard.tryAcquire()) {
            log.warn("本地AI并发已满, taskId={}", task.getId());
            return ConsumeResult.FAILURE;
        }

        String workerId = "diary-ai-" + UUID.randomUUID();
        String permitId = task.getId() + ":" + workerId;
        boolean globalPermitAcquired = false;
        try {
            try {
                globalPermitAcquired = globalPermitManager.tryAcquire(model, permitId);
            } catch (RuntimeException redisException) {
                log.warn("Redis全局信号量不可用, taskId={}, errorCode={}",
                        task.getId(), AiTaskErrorCodeEnum.REDIS_UNAVAILABLE, redisException);
                return ConsumeResult.FAILURE;
            }
            if (!globalPermitAcquired) {
                log.info("模型全局并发已满, taskId={}, model={}, errorCode={}",
                        task.getId(), model, AiTaskErrorCodeEnum.MODEL_CAPACITY_EXHAUSTED);
                return ConsumeResult.FAILURE;
            }

            AiTaskPO claimedTask = claimTask(message, workerId);
            if (claimedTask == null) return ConsumeResult.SUCCESS;
            return invokeModel(message, claimedTask, model, permitId);
        } finally {
            if (globalPermitAcquired) releaseGlobalPermit(model, permitId, task.getId());
            localConcurrencyGuard.release();
        }
    }

    private AiTaskPO claimTask(AiTaskMessageDto message, String workerId) {
        AiTaskProcessDto claim = AiTaskProcessDto.builder()
                .taskId(message.getTaskId())
                .userId(message.getUserId())
                .clientRequestId(message.getClientRequestId())
                .workerId(workerId)
                .leaseSeconds(properties.getTask().getExecutionLeaseSeconds())
                .build();
        if (diaryAiMapper.claimForExecution(claim) != 1) {
            // 数据库CAS是任务所有权的最终防线；抢占失败说明重复消息已由其他实例处理。
            return null;
        }
        return diaryAiMapper.selectAiTaskByTaskId(message.getTaskId());
    }

    private ConsumeResult invokeModel(AiTaskMessageDto message, AiTaskPO task,
                                      AIEnum model, String permitId) {
        try (AiTaskLeaseRenewer.LeaseRenewalHandle ignoredTaskLease =
                     taskLeaseRenewer.start(task.getId(), task.getWorkerId(), task.getVersionId());
             RedisModelPermitRenewer.PermitRenewalHandle ignoredGlobalPermit =
                     globalPermitRenewer.start(model, permitId)) {
            aiTaskExecutor.execute(message, task);
            return ConsumeResult.SUCCESS;
        } catch (Exception executionException) {
            try {
                return transactionService.handleExecutionFailure(
                        message, task, task.getWorkerId(), executionException);
            } catch (RuntimeException persistException) {
                log.error("AI任务失败状态保存失败, taskId={}", task.getId(), persistException);
                return ConsumeResult.FAILURE;
            }
        }
    }

    private AiTaskMessageDto parseMessage(MessageView messageView) {
        try {
            ByteBuffer body = messageView.getBody();
            byte[] bytes = new byte[body.remaining()];
            body.get(bytes);
            AiTaskMessageDto message = objectMapper.readValue(bytes, AiTaskMessageDto.class);
            validateMessage(message);
            return message;
        } catch (Exception exception) {
            // 消息格式错误不会通过重试自行恢复，记录后直接ACK，避免制造无意义DLQ噪声。
            log.error("AI任务消息格式错误，已忽略, messageId={}, reason={}",
                    messageView.getMessageId(), exception.getMessage());
            return null;
        }
    }

    private void validateMessage(AiTaskMessageDto message) {
        if (message == null
                || message.getEventId() == null || message.getEventId().isBlank()
                || message.getTaskId() == null
                || message.getUserId() == null
                || message.getClientRequestId() == null || message.getClientRequestId().isBlank()
                || !Objects.equals(OUTBOX_SCHEMA_VERSION, message.getSchemaVersion())
                || !AiTaskTypeEnum.isTrueTaskType(message.getTaskType())) {
            throw new IllegalArgumentException("AI任务消息字段或协议版本不合法");
        }
    }

    private boolean isTerminal(String status) {
        return AiTaskStatusEnum.SUCCESS.name().equals(status)
                || AiTaskStatusEnum.FAILED.name().equals(status)
                || AiTaskStatusEnum.CANCELLED.name().equals(status)
                || AiTaskStatusEnum.DEAD_LETTER.name().equals(status);
    }

    private void releaseGlobalPermit(AIEnum model, String permitId, Long taskId) {
        try {
            globalPermitManager.release(model, permitId);
        } catch (RuntimeException exception) {
            // Redis租约会自动回收许可，这里不再增加第二套补偿流程。
            log.warn("释放Redis全局信号量失败，等待租约过期, taskId={}", taskId, exception);
        }
    }
}
