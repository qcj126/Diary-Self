package diary.diaryai.outbox;

import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox 保留策略：分批清理已确认发送的历史记录，DEAD 记录永不自动删除。
 * 删除sent记录是因为broker已确认消息收到，且任务已成功被消费者接收，因此可以删除，至于消费者是否成功处理，由重试机制保证，与outbox已无关。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiOutboxMaintenanceJob {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskProperties properties;

    @Scheduled(fixedDelayString = "${diary.ai.rocketmq.cleanup-interval-ms:3600000}")
    public void cleanupSentOutbox() {
        int deleted = diaryAiMapper.deleteExpiredSentOutbox(
                properties.getRocketmq().getSentRetentionDays(),
                properties.getRocketmq().getCleanupBatchSize());
        if (deleted > 0) {
            log.info("已清理过期SENT Outbox, count={}, retentionDays={}",
                    deleted, properties.getRocketmq().getSentRetentionDays());
        }
    }
}
