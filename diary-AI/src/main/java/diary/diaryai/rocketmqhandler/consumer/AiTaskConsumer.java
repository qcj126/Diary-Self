package diary.diaryai.rocketmqhandler.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.ai.dto.AiTaskMessageDto;
import diary.common.entity.ai.dto.AiTaskProcessDto;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.enums.aienum.AiTaskErrorCodeEnum;
import diary.common.enums.aienum.AiTaskStatusEnum;
import diary.diaryai.executor.AiTaskExecutor;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.annotation.RocketMQMessageListener;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.core.RocketMQListener;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static diary.common.consts.AiTaskConst.MAX_ERROR_MSG_LENGTH;
import static diary.common.consts.AiTaskConst.OUTBOX_SCHEMA_VERSION;

@Service
@RocketMQMessageListener(
        consumerGroup = "diary-ai-qwen-plus-worker-v1",
        topic = "diary-ai-task",
        tag = "QWEN_PLUS_NUTRIENT"
)
@RequiredArgsConstructor
@Slf4j
public class AiTaskConsumer implements RocketMQListener {
    private final AiTaskProperties aiTaskProperties;
    private final ObjectMapper objectMapper;
    private final AiTaskExecutor aiTaskExecutor;
    private final DiaryAiMapper diaryAiMapper;

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

        final String workerId = "diary-ai-" + UUID.randomUUID();
        final LocalDateTime now = LocalDateTime.now();
        AiTaskProcessDto claimRequest = AiTaskProcessDto.builder()
                .taskId(message.getTaskId())
                .userId(message.getUserId())
                .clientRequestId(message.getClientRequestId())
                .workerId(workerId)
                .queueTime(now)
                .startTime(now)
                .leaseUntil(now.plus(aiTaskProperties.getTask().getLeaseDuration()))
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

        try {
            boolean executed = aiTaskExecutor.execute(message, claimedTask);
            return executed ? ConsumeResult.SUCCESS : handleOwnershipLost(message.getTaskId());
        } catch (RuntimeException executionException) {
            return handleExecutionFailure(message, claimedTask, workerId, executionException);
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
            AiTaskProcessDto exhaustedRequest = AiTaskProcessDto.builder()
                    .taskId(currentTask.getId())
                    .userId(currentTask.getUserId())
                    .clientRequestId(currentTask.getClientRequestId())
                    .versionId(currentTask.getVersionId())
                    .errorCode(AiTaskErrorCodeEnum.RETRY_EXHAUSTED.name())
                    .errorMessage(AiTaskErrorCodeEnum.RETRY_EXHAUSTED.getDisplayName())
                    .finishTime(LocalDateTime.now())
                    .build();
            int failed = diaryAiMapper.markFailedIfAttemptsExhausted(exhaustedRequest);
            if (failed == 1) {
                return ConsumeResult.SUCCESS;
            }
        }

        /*
         * 抢占失败通常意味着另一个 Consumer 已持有有效租约。当前消息只是重复副本，ACK 不会丢失任务，
         * 因为真正的持有者仍在执行；若持有者宕机，租约过期后的后续重投/恢复任务仍可接管。
         */
        return handleOwnershipLost(message.getTaskId());
    }

    private ConsumeResult handleExecutionFailure(AiTaskMessageDto message, AiTaskPO claimedTask, String workerId, RuntimeException executionException) {
        boolean permanentError = executionException instanceof IllegalArgumentException;
        boolean attemptsExhausted = claimedTask.getAttemptCount() >= claimedTask.getMaxAttempts();
        String errorMessage = truncateErrorMessage(executionException.getMessage());

        AiTaskProcessDto failureRequest = AiTaskProcessDto.builder()
                .taskId(message.getTaskId())
                .userId(message.getUserId())
                .clientRequestId(message.getClientRequestId())
                .workerId(workerId)
                .versionId(claimedTask.getVersionId())
                .errorCode(
                        permanentError ? AiTaskErrorCodeEnum.PERMANENT_ERROR.name() :
                        attemptsExhausted ? AiTaskErrorCodeEnum.RETRY_EXHAUSTED.name() : AiTaskErrorCodeEnum.RETRYABLE_ERROR.name())
                .errorMessage(errorMessage)
                .finishTime(LocalDateTime.now())
                .build();

        try {
            if (permanentError || attemptsExhausted) {
                /*
                 * 永久错误或业务模型调用次数耗尽时写 FAILED 并 ACK。继续返回 FAILURE 只会让 Broker
                 * 重复投递一个已确认无法继续执行的任务，混淆“业务失败”和“消息进入 DLQ”两种语义。
                 */
                int failed = diaryAiMapper.markFailedIfOwned(failureRequest);
                log.error("AI task failed permanently, taskId={}, attemptCount={}",
                        message.getTaskId(), claimedTask.getAttemptCount(), executionException);
                return failed == 1 ? ConsumeResult.SUCCESS : handleOwnershipLost(message.getTaskId());
            }

            int retryWaiting = diaryAiMapper.markRetryWaitIfOwned(failureRequest);
            log.warn("AI task will be retried, taskId={}, attemptCount={}",
                    message.getTaskId(), claimedTask.getAttemptCount(), executionException);
            return retryWaiting == 1 ? ConsumeResult.FAILURE : handleOwnershipLost(message.getTaskId());
        } catch (RuntimeException statusUpdateException) {
            log.error("Failed to persist AI task failure state, taskId={}",
                    message.getTaskId(), statusUpdateException);
            return ConsumeResult.FAILURE;
        }
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

    private String truncateErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "未提供异常信息";
        }
        return message.length() <= MAX_ERROR_MSG_LENGTH ? message : message.substring(0, MAX_ERROR_MSG_LENGTH);
    }
}
