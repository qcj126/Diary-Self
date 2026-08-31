package diary.diaryai.impl;

import diary.common.entity.mq.po.MqOutboxPO;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.enums.outbox.OutboxEventTypeEnum;
import diary.common.enums.outbox.OutboxStatusEnum;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.service.AiOutboxService;
import diary.diaryai.service.AiTaskCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.util.StringUtils.truncate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiOutboxServiceImpl implements AiOutboxService {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskProperties properties;
    private final AiTaskCommandService aiTaskCommandService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean claim(MqOutboxPO outbox) {
        int changed = diaryAiMapper.claimOutbox(outbox.getId(), outbox.getVersionId(), LocalDateTime.now());
        if (changed == 1) {
            // 数据库中已经将outbox状态改为sending，为后续步骤，需要设置outbox状态为sending，并且版本号+1
            outbox.setStatus(OutboxStatusEnum.SENDING.name());
            outbox.setVersionId(outbox.getVersionId() + 1);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmSent(MqOutboxPO outbox, String brokerMessageId) {
        LocalDateTime now = LocalDateTime.now();
        int sent = diaryAiMapper.markOutboxSent(outbox.getId(), outbox.getVersionId(), brokerMessageId, now);
        if (sent != 1) {
            throw new IllegalStateException("Outbox SENT 更新失败: " + outbox.getId());
        }

        if (OutboxEventTypeEnum.AI_TASK_CREATED.name().equals(outbox.getEventType())) {
            // 消息发送完毕之后再将任务状态改为queued
            diaryAiMapper.markQueuedByTaskIdIfPending(outbox.getAggregateId(), now);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordFailure(MqOutboxPO outbox, Throwable error) {
        LocalDateTime now = LocalDateTime.now();
        int nextRetryCount = outbox.getRetryCount() + 1;
        String lastError = truncate(error.getMessage(), 1000);

        int changed;
        /*
         * 改前：maxRetries=3 时原始发送加起来总共只执行 3 次，字段名“最大重试次数”与实际语义不一致。
         * 改后：首次发送不算重试，失败次数超过 maxRetries 才进入 DEAD，即总尝试次数为 1 + maxRetries。
         */
        if (nextRetryCount > outbox.getMaxRetries()) {
            changed = diaryAiMapper.markOutboxDead(outbox.getId(), outbox.getVersionId(), lastError, now);
        } else {
            changed = diaryAiMapper.markOutboxRetry(outbox.getId(), outbox.getVersionId(), calculateNextRetry(outbox.getRetryCount()), lastError, now);
        }
        if (changed != 1) {
            throw new IllegalStateException("Outbox失败状态更新失败: " + outbox.getId());
        }

        if (nextRetryCount > outbox.getMaxRetries() && isTaskDispatchEvent(outbox)) {
            /*
             * 改前：AI_TASK_CREATED/AI_TASK_RETRY Outbox 进入 DEAD 后，task 不发生变化，永久卡在等待态。
             * 改后：同一事务内将仍处于 PENDING/QUEUED/RETRY_WAIT 的任务收敛到 DEAD_LETTER，
             * 并追加 AI_FAILED Outbox；若任务已 RUNNING/终态，CAS 不会覆盖其新状态。
             * 效果：投递失败具有明确业务终态，同时保留“Broker 实际已收到但生产者未知”场景下的并发安全。
             */
            AiTaskPO task = diaryAiMapper.selectAiTaskByTaskId(outbox.getAggregateId());
            if (task != null) {
                boolean deadLettered = aiTaskCommandService.deadLetterDispatchTask(
                        task,
                        "任务消息Outbox重试耗尽, outboxId=" + outbox.getId()
                );
                log.error("任务投递Outbox已进入DEAD, outboxId={}, taskId={}, taskDeadLettered={}",
                        outbox.getId(), outbox.getAggregateId(), deadLettered);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverSendingTimeout() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeoutBefore = now.minusSeconds(properties.getRocketmq().getPublisherSendingTimeoutSeconds());
        diaryAiMapper.recoverSendingTimeout(timeoutBefore, now);
    }

    private LocalDateTime calculateNextRetry(int currentRetryCount) {
        long baseSeconds = Math.min(1L << Math.min(currentRetryCount, 7), 120L) * 5L;
        long jitterSeconds = ThreadLocalRandom.current().nextLong(0, 4);
        return LocalDateTime.now().plusSeconds(
                Math.min(baseSeconds, 600L) + jitterSeconds);
    }

    private boolean isTaskDispatchEvent(MqOutboxPO outbox) {
        return OutboxEventTypeEnum.AI_TASK_CREATED.name().equals(outbox.getEventType())
                || OutboxEventTypeEnum.AI_TASK_RETRY.name().equals(outbox.getEventType());
    }
}
