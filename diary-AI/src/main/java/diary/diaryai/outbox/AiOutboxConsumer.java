package diary.diaryai.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.diaryai.entity.ai.dto.AiTaskMessageDto;
import diary.diaryai.entity.ai.dto.AiTaskProcessDto;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.enums.aienum.AiTaskStatusEnum;
import diary.diaryai.enums.aienum.AiTaskTypeEnum;
import diary.diaryai.executor.AiTaskExecutor;
import diary.diaryai.guard.LocalAiConcurrencyGuard;
import diary.diaryai.guard.AiTaskLeaseRenewer;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
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
@RocketMQMessageListener(
        consumerGroup = "${diary.ai.rocketmq.task-consumer-nutrient-group:diary-ai-nutrient-workers}",
        topic = "${diary.ai.rocketmq.task-topic:diary-ai-task}",
        tag = "${diary.ai.rocketmq.task-nutrient-tag:NUTRIENT_ANALYSE}",
        sslEnabled = false
)
@RequiredArgsConstructor
@Slf4j
public class AiOutboxConsumer implements RocketMQListener {
    private final AiTaskProperties aiTaskProperties;
    private final ObjectMapper objectMapper;
    private final AiTaskExecutor aiTaskExecutor;
    private final DiaryAiMapper diaryAiMapper;
    private final LocalAiConcurrencyGuard localAiConcurrencyGuard;
    private final AiTaskLeaseRenewer aiTaskLeaseRenewer;
    private final AiTransactionService aiTransactionService;

    @Override
    public ConsumeResult consume(MessageView messageView) {
        ByteBuffer body = messageView.getBody();
        byte[] bytes = new byte[body.remaining()];
        body.get(bytes); // 将ByteBuffer数据拷贝到byte数组中的真正操作

        final AiTaskMessageDto message;
        try {
            message = objectMapper.readValue(bytes, AiTaskMessageDto.class);
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

            // 长时间模型调用期间持续续租，提交结果仍校验 workerId + versionId。
            /*
            * 续租中的定时任务与关闭时序：
            *   进入 try 块
            *   → start() 创建定时续租 future
            *   → 开始同步调用模型
            *   → 模型调用结束，execute() 返回
            *   → 离开 try 块
            *   → Java 自动调用 ignored.close()
            *   → close() 执行 future.cancel(false)
            *   → 后续不再续租
            * */
            try (AiTaskLeaseRenewer.LeaseRenewalHandle ignored = aiTaskLeaseRenewer.start(
                    claimedTask.getId(), workerId, claimedTask.getVersionId())) {
                boolean executed = aiTaskExecutor.execute(message, claimedTask);
                return executed ? ConsumeResult.SUCCESS : handleOwnershipLost(message.getTaskId());
            } catch (Exception executionException) {
                try {
                    return aiTransactionService.handleExecutionFailure(
                            message, claimedTask, workerId, executionException);
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
            /*
            * 消息引用的 AI 任务不存在：可能是误投、跨环境消息、数据被清理或读库延迟
            * 可能的异常场景包括：
            *       - 消息是其他服务、测试脚本或人工误投到该 Topic 的；
            *       - 消息体的 taskId 错误、被篡改或协议版本不兼容；
            *       - 有人手工删除/清理了 ai_task，但 Broker 中仍保留旧消息；
            *       - 消息来自另一套环境，却误连到了当前环境的 Broker；
            *       - 读写分离时，查询误读到了尚未同步的从库；
            *       - 极少见的数据损坏或历史迁移遗漏。
            * */
            log.error("AI task does not exist, taskId={}", message.getTaskId());
            return ConsumeResult.FAILURE;
        }

        if (isTerminal(currentTask.getStatus())) {
            /* 重复消息遇到任务终态直接 ACK，避免 SUCCESS/FAILED 任务再次调用模型。 */
            /*
            * 如果这里返回 ConsumeResult.FAILURE，RocketMQ 会认为“消费者处理失败”，然后：
            * 同一条消息重投
            *    → 又查到任务是 SUCCESS / FAILED
            *    → 又返回 FAILURE
            *    → 不断重试
            *    → 达到重试上限后进入 DLQ
            * 这不会带来任何新的业务进展，只会造成：
            *    - 无意义的消息重投和日志噪声；
            *    - 消费积压；
            *    - 无意义的 DLQ 告警和人工排查；
            *    - 某些情况下占用 Consumer 处理能力。
            * */
            return ConsumeResult.SUCCESS;
        }

        if (currentTask.getAttemptCount() != null
                && currentTask.getMaxAttempts() != null
                && currentTask.getAttemptCount() >= currentTask.getMaxAttempts()) {
            if (aiTransactionService.failExhaustedTask(
                    currentTask, "消息重投到达时任务执行次数已耗尽")) {
                return ConsumeResult.SUCCESS;
            }
        }

        /*
         * 多consumer的情况下：
         * 抢占失败通常意味着另一个 Consumer 已持有有效租约。当前消息只是重复副本，ACK 不会丢失任务，
         * 因为真正的持有者仍在执行；若持有者宕机，租约过期后的后续重投/恢复任务仍可接管。
         *
         * 什么情况下会进行重投：
         *      1：RocketMQ 自己的重投
         *          触发条件是 Broker 没有得到这条消息的成功消费确认，例如：
         *              - Consumer A 返回 ConsumeResult.FAILURE；
         *              - Consumer A 调用过程中进程崩溃；
         *              - Consumer A 已处理完，但成功确认因网络中断没有到达 Broker；
         *              - Consumer A 与 Broker 断连，Consumer Group 发生 Rebalance，Queue 被分配给 Consumer B；
         *              - 消费超时、客户端异常等。
         *      2：租约过期后的应用层恢复
         *          租约过期后，代码中将任务的状态由RUNNING改为RETRY_WAIT
         *          然后新建一条重试的outbox消息进行投递
         *          所以租约过期 ≠ RocketMQ 自动重投
         *          租约过期
         *              → diary-ai 恢复任务生成新的重试消息
         *              → RocketMQ 再投递
         *      3：Outbox 发送重试
         *          典型故障窗口：
         *              Publisher → Broker 发送消息成功
         *              Broker 已落盘
         *              ↓
         *              Publisher 在写 mq_outbox = SENT 前宕机或网络超时
         *              ↓
         *              Publisher 不知道 Broker 到底有没有收到
         *              ↓
         *              SENDING 超时后，Outbox 重试发送
         *              ↓
         *              Broker 可能收到第二条指向同一 taskId 的消息
         *          典型故障结果：
         *              消息 M1 → Consumer A → A 抢占并执行
         *              消息 M2 → Consumer B → B 抢占失败
         *              → B 发现任务属于 A
         *              → handleOwnershipLost()
         *              → B ACK M2
         *      4：消息人工重投
         *          运维通过 RocketMQ 控制台重发；
         *          管理后台提供“重新执行 AI 任务”；
         *          补偿程序重新写入 Retry Outbox；
         *          Consumer Group 的 Offset 被重置，重新消费历史消息。
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
}
