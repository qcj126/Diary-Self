package diary.diaryai.outbox;

import diary.common.entity.mq.po.MqOutboxPO;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.redis.AiTaskCacheService;
import diary.diaryai.service.AiOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.apache.rocketmq.client.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiOutboxPublisher {
    private final DiaryAiMapper diaryAiMapper;
    private final AiOutboxService aiOutboxService;
    private final AiTaskProperties properties;
    private final RocketMQClientTemplate rocketMQClientTemplate;
    private final AiTaskCacheService aiTaskCacheService;

    @Scheduled(fixedDelayString = "${diary.ai.rocketmq.publisher-interval-ms:1000}")
    public void publishReadyMessages() {
        int cnt = aiOutboxService.recoverSendingTimeout();
        log.info("恢复了 {} 条数据", cnt);
        if (cnt == 0) return;

        List<MqOutboxPO> batch = diaryAiMapper.selectReadyOutbox(
                LocalDateTime.now(),
                properties.getRocketmq().getPublisherBatchSize());

        for (MqOutboxPO outbox : batch) {
            if (!aiOutboxService.claim(outbox)) {
                continue;
            }
            try {
                Message<String> message = MessageBuilder
                        .withPayload(outbox.getPayload())
                        .setHeader(RocketMQHeaders.KEYS, outbox.getMessageKey())
                        .build();
                String destination = outbox.getTopic() + ":" + outbox.getTag();
                SendReceipt sendReceipt = rocketMQClientTemplate.syncSendNormalMessage(destination, message);
                aiOutboxService.confirmSent(outbox, sendReceipt.getMessageId().toString());
            } catch (Exception e) {
                log.error("Outbox发送失败, outboxId={}, eventId={}",
                        outbox.getId(), outbox.getEventId(), e);
                aiOutboxService.recordFailure(outbox, e);
            }
        }
    }
}
