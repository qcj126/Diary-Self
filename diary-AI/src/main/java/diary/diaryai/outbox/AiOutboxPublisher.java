package diary.diaryai.outbox;

import diary.common.entity.mq.po.MqOutboxPO;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
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

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiOutboxPublisher {
    private final DiaryAiMapper diaryAiMapper;
    private final AiOutboxService aiOutboxService;
    private final AiTaskProperties properties;
    private final RocketMQClientTemplate rocketMQClientTemplate;

    @Scheduled(fixedDelayString = "${diary.ai.rocketmq.publisher-interval-ms:1000}")
    public void publishReadyMessages() {
        recoverTimedOutMessages();
        List<MqOutboxPO> batch = diaryAiMapper.selectReadyOutbox(properties.getRocketmq().getPublisherBatchSize());

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
                try {
                    aiOutboxService.recordFailure(outbox, e);
                } catch (RuntimeException stateException) {
                    log.error("Outbox失败状态落库异常, 等待SENDING超时恢复, outboxId={}",
                            outbox.getId(), stateException);
                }
            }
        }
    }

    private void recoverTimedOutMessages() {
        List<MqOutboxPO> timedOut = diaryAiMapper.selectTimedOutbox(
                properties.getRocketmq().getPublisherSendingTimeoutSeconds(),
                properties.getRocketmq().getPublisherBatchSize());
        for (MqOutboxPO outbox : timedOut) {
            try {
                /* 超时表示发送结果未知，必须计入重试上限，否则会无限重复发送。 */
                aiOutboxService.recoverSendingTimeout(outbox);
            } catch (RuntimeException e) {
                log.error("恢复超时Outbox失败, outboxId={}", outbox.getId(), e);
            }
        }
    }
}
