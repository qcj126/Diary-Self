package diary.diaryai.outbox;

import diary.common.entity.mq.po.MqOutboxPO;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.service.AiOutboxService;
import diary.diaryai.service.OutboxMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
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
    private final OutboxMessageProducer producer;
    private final AiTaskProperties properties;

    @Scheduled(fixedDelayString = "${diary.ai.rocketmq.publisher-interval-ms:1000}")
    public void publishReadyMessages() {
        aiOutboxService.recoverSendingTimeout();

        List<MqOutboxPO> batch = diaryAiMapper.selectReadyOutbox(
                LocalDateTime.now(),
                properties.getRocketmq().getPublisherBatchSize());

        for (MqOutboxPO outbox : batch) {
            if (!aiOutboxService.claim(outbox)) {
                continue;
            }
            try {
                SendReceipt receipt = producer.send(outbox);
                aiOutboxService.confirmSent(outbox, receipt.getMessageId().toString());
            } catch (RuntimeException e) {
                log.error("Outbox发送失败, outboxId={}, eventId={}",
                        outbox.getId(), outbox.getEventId(), e);
                aiOutboxService.recordFailure(outbox, e);
            }
        }
    }
}
