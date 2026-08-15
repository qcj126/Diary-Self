package diary.diaryai.service;

import diary.common.entity.mq.po.MqOutboxPO;
import org.apache.rocketmq.client.apis.producer.SendReceipt;

public interface OutboxMessageProducer {
    SendReceipt send(MqOutboxPO outbox);
}
