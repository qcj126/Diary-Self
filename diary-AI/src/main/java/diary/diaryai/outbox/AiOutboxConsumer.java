package diary.diaryai.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.ai.dto.AiTaskMessageDto;
import diary.common.entity.ai.dto.AiTaskProcessDto;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.enums.aienum.AiTaskStatusEnum;
import diary.diaryai.executor.AiTaskExecutor;
import diary.diaryai.guard.LocalAiConcurrencyGuard;
import diary.diaryai.guard.AiTaskLeaseRenewer;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.redis.AiTaskCacheService;
import diary.diaryai.service.AiTaskCommandService;
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

import static diary.common.consts.AiTaskConst.MAX_ERROR_MSG_LENGTH;
import static diary.common.consts.AiTaskConst.OUTBOX_SCHEMA_VERSION;

@Service
@RocketMQMessageListener(
        consumerGroup = "${diary.ai.rocketmq.task-consumer-group:diary-ai-qwen-plus-worker-v2}",
        topic = "${diary.ai.rocketmq.task-topic:diary-ai-task}",
        tag = "${diary.ai.rocketmq.task-tag:QWEN_PLUS_NUTRIENT}",
        sslEnabled = false
)
@RequiredArgsConstructor
@Slf4j
public class AiOutboxConsumer implements RocketMQListener {
    private final AiTaskProperties aiTaskProperties;
    private final ObjectMapper objectMapper;
    private final AiTaskExecutor aiTaskExecutor;
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskCacheService aiTaskCacheService;
    private final LocalAiConcurrencyGuard localAiConcurrencyGuard;
    private final AiTaskLeaseRenewer aiTaskLeaseRenewer;
    private final AiTaskCommandService aiTaskCommandService;

    @Override
    public ConsumeResult consume(MessageView messageView) {
        ByteBuffer bodyBuffer = messageView.getBody();
        byte[] body = new byte[bodyBuffer.remaining()];
        bodyBuffer.get(body);

        final AiTaskMessageDto message;
        try {
            message = objectMapper.readValue(body, AiTaskMessageDto.class);
            validateMessage(message);
        } catch (Exception parseException) {
            /*
             * 以前只捕获 IOException，schemaVersion、taskId 等协议错误没有明确分类，可能直接穿透 Listener。
             * 这类消息无法定位到可靠任务，返回 FAILURE 让 RocketMQ 按消费策略有限重试并最终进入 DLQ。
             */
            log.error("Invalid AI task message, messageId={}", messageView.getMessageId(), parseException);
            return ConsumeResult.FAILURE;
        }

        // 在任务进入处理前，执行并发保护
        if (!localAiConcurrencyGuard.tryAcquire()) {
            log.warn("本地AI并发已满, taskId={}", message.getTaskId());
            return ConsumeResult.FAILURE;
        }

        try {
            final String workerId = "diary-ai-" + UUID.randomUUID();
            AiTaskProcessDto claimRequest = AiTaskProcessDto.builder()
                    .taskId(message.getTaskId())
                    .userId(message.getUserId())
                    .clientRequestId(message.getClientRequestId())
                    .workerId(workerId)
                    .leaseSeconds(aiTaskProperties.getTask().getExecutionLeaseSeconds())
                    .build();

            final int claimed;
            try {
                claimed = diaryAiMapper.claimForExecution(claimRequest);
            } catch (RuntimeException databaseException) {
                log.error("Failed to claim AI task, taskId={}", message.getTaskId(), databaseException);
                return ConsumeResult.FAILURE;
            }

            if (claimed != 1) {
                return handleUnclaimedMessage(message);
            }
            aiTaskCacheService.evict(message.getTaskId());
            // 再一次确认此任务属于当前的workId
            final AiTaskPO claimedTask;
            try {
                claimedTask = diaryAiMapper.selectAiTaskByTaskId(message.getTaskId());
            } catch (RuntimeException e) {
                log.error("Failed to confirm AI task ownership, taskId={}",
                        message.getTaskId(), e);
                return ConsumeResult.FAILURE;
            }
            if (claimedTask == null
                    || !AiTaskStatusEnum.RUNNING.name().equals(claimedTask.getStatus())
                    || !Objects.equals(workerId, claimedTask.getWorkerId())) {
                log.warn("AI task was claimed but ownership cannot be confirmed, taskId={}, workerId={}",
                        message.getTaskId(), workerId);
                return ConsumeResult.FAILURE;
            }

            /*
             * 改前：RUNNING 租约只在抢占时写一次；AI调用超过固定租约后，Recovery 会启动第二个昂贵的模型调用。
             * 改后：执行期间按租约的 1/3 周期续期，且续期必须匹配 workerId + versionId。
             * 效果：正常长任务不会被误接管；旧 Worker 仍无法覆盖新 Worker 的结果。
             */
            try (AiTaskLeaseRenewer.LeaseRenewalHandle ignored = aiTaskLeaseRenewer.start(
                    claimedTask.getId(), workerId, claimedTask.getVersionId())) {
                boolean executed = aiTaskExecutor.execute(message, claimedTask);
                aiTaskCacheService.evict(message.getTaskId());
                return executed ? ConsumeResult.SUCCESS : handleOwnershipLost(message.getTaskId());
            } catch (Exception executionException) {
                try {
                    ConsumeResult result = aiTaskCommandService.handleExecutionFailure(
                            message, claimedTask, workerId, executionException);
                    aiTaskCacheService.evict(message.getTaskId());
                    return result;
                } catch (RuntimeException failureStateException) {
                    /*
                     * handleExecutionFailure 的事务已在异常抛出前完成回滚。
                     * Listener 在事务外将其转换为 FAILURE，交给 RocketMQ 后续重投。
                     */
                    log.error("Failed to persist AI task failure state, taskId={}",
                            message.getTaskId(), failureStateException);
                    return ConsumeResult.FAILURE;
                }
            }
        } finally {
            localAiConcurrencyGuard.release();
        }
    }

    private ConsumeResult handleUnclaimedMessage(AiTaskMessageDto message) {
        AiTaskPO currentTask = diaryAiMapper.selectAiTaskByTaskId(message.getTaskId());
        if (currentTask == null) {
            // 另一个消费者尝试抢占此条消息但是失败了
            log.error("AI task does not exist, taskId={}", message.getTaskId());
            return ConsumeResult.FAILURE;
        }

        if (isTerminal(currentTask.getStatus())) {
            /* 重复消息遇到终态直接 ACK，避免 SUCCESS/FAILED 任务再次调用模型。 */
            return ConsumeResult.SUCCESS;
        }

        if (currentTask.getAttemptCount() != null
                && currentTask.getMaxAttempts() != null
                && currentTask.getAttemptCount() >= currentTask.getMaxAttempts()) {
            /*
             * 改前：这里直接 UPDATE FAILED 后 ACK，没有创建 AI_FAILED Outbox，形成“有终态、无终态事件”。
             * 改后：调用统一事务服务，同时完成 FAILED 与 AI_FAILED Outbox；CAS 失败说明其他 Worker 已接管。
             */
            if (aiTaskCommandService.failExhaustedTask(
                    currentTask, "消息重投到达时任务执行次数已耗尽")) {
                aiTaskCacheService.evict(currentTask.getId());
                return ConsumeResult.SUCCESS;
            }
        }

        /*
         * 抢占失败通常意味着另一个 Consumer 已持有有效租约。当前消息只是重复副本，ACK 不会丢失任务，
         * 因为真正的持有者仍在执行；若持有者宕机，租约过期后的后续重投/恢复任务仍可接管。
         */
        return handleOwnershipLost(message.getTaskId());
    }

    private ConsumeResult handleOwnershipLost(Long taskId) {
        AiTaskPO currentTask = diaryAiMapper.selectAiTaskByTaskId(taskId);
        if (currentTask != null) {
            log.info("AI task is owned or completed elsewhere, taskId={}, status={}, workerId={}, version={}",
                    taskId, currentTask.getStatus(), currentTask.getWorkerId(), currentTask.getVersionId());
            return ConsumeResult.SUCCESS;
        }
        return ConsumeResult.FAILURE;
    }

    // 再次校验消息，防止消息在传输过程中被篡改，或者其他功能的生产者误发消息到本消费者
    // 也可以防止某些非法调用
    private void validateMessage(AiTaskMessageDto message) {
        if (message == null
                || message.getTaskId() == null
                || message.getUserId() == null
                || message.getClientRequestId() == null
                || message.getClientRequestId().isBlank()
                || !Objects.equals(OUTBOX_SCHEMA_VERSION, message.getSchemaVersion())
                || !aiTaskProperties.getRocketmq().getTaskTag().equals(message.getTaskType())) {
            throw new IllegalArgumentException("AI任务消息字段或协议版本不合法");
        }
    }

    private boolean isTerminal(String status) {
        return AiTaskStatusEnum.SUCCESS.name().equals(status)
                || AiTaskStatusEnum.FAILED.name().equals(status)
                || AiTaskStatusEnum.CANCELLED.name().equals(status)
                || AiTaskStatusEnum.DEAD_LETTER.name().equals(status);
    }
}
