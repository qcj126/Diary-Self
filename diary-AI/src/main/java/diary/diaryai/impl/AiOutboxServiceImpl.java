package diary.diaryai.impl;

import diary.common.entity.mq.po.MqOutboxPO;
import diary.common.enums.outbox.OutboxEventTypeEnum;
import diary.common.enums.outbox.OutboxStatusEnum;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.service.AiOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.util.StringUtils.truncate;

@Service
@RequiredArgsConstructor
public class AiOutboxServiceImpl implements AiOutboxService {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskProperties properties;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean claim(MqOutboxPO outbox) {
        int changed = diaryAiMapper.claimOutbox(
                outbox.getId(), outbox.getVersionId(), LocalDateTime.now());
        if (changed == 1) {
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
        int sent = diaryAiMapper.markOutboxSent(
                outbox.getId(), outbox.getVersionId(), brokerMessageId, now);
        if (sent != 1) {
            throw new IllegalStateException("Outbox SENT 更新失败: " + outbox.getId());
        }

        if (OutboxEventTypeEnum.AI_TASK_CREATED.name().equals(outbox.getEventType())) {
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
        if (nextRetryCount >= outbox.getMaxRetries()) {
            changed = diaryAiMapper.markOutboxDead(
                    outbox.getId(), outbox.getVersionId(), lastError, now);
        } else {
            changed = diaryAiMapper.markOutboxRetry(
                    outbox.getId(),
                    outbox.getVersionId(),
                    calculateNextRetry(outbox.getRetryCount()),
                    lastError,
                    now);
        }
        if (changed != 1) {
            throw new IllegalStateException("Outbox失败状态更新失败: " + outbox.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recoverSendingTimeout() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeoutBefore = now.minusSeconds(
                properties.getRocketmq().getPublisherSendingTimeoutSeconds());
        return diaryAiMapper.recoverSendingTimeout(timeoutBefore, now);
    }

    private LocalDateTime calculateNextRetry(int currentRetryCount) {
        long baseSeconds = Math.min(1L << Math.min(currentRetryCount, 7), 120L) * 5L;
        long jitterSeconds = ThreadLocalRandom.current().nextLong(0, 4);
        return LocalDateTime.now().plusSeconds(
                Math.min(baseSeconds, 600L) + jitterSeconds);
    }
}
