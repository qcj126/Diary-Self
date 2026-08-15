package diary.diaryai.rocketmqhandler.producer;

import diary.common.entity.mq.po.MqOutboxPO;
import diary.diaryai.service.OutboxMessageProducer;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.apache.rocketmq.client.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RocketMqOutboxProducer implements OutboxMessageProducer {
    private final RocketMQClientTemplate rocketMQClientTemplate;

    @Override
    public SendReceipt send(MqOutboxPO outbox) {
        Message<String> message = MessageBuilder
                .withPayload(outbox.getPayload())
                .setHeader(RocketMQHeaders.KEYS, outbox.getMessageKey())
                .build();

        String destination = outbox.getTopic() + ":" + outbox.getTag();
        return rocketMQClientTemplate.syncSendNormalMessage(destination, message);
    }
}
