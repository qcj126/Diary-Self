package diary.diaryai.service;

import diary.common.entity.mq.po.MqOutboxPO;

public interface AiOutboxService {
    boolean claim(MqOutboxPO outbox);

    void confirmSent(MqOutboxPO sendingOutbox, String brokerMessageId);

    void recordFailure(MqOutboxPO sendingOutbox, Throwable error);

    void recoverSendingTimeout(MqOutboxPO timedOutOutbox);
}
