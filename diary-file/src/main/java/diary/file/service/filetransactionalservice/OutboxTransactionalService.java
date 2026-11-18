package diary.file.service.filetransactionalservice;

import diary.file.entity.mq.po.FileMqOutboxPO;

public interface OutboxTransactionalService {
    void recoverTimedOutForRetry(FileMqOutboxPO fileMqOutboxPO);

    boolean claimForPublishing(Long id, Integer versionId);

    boolean markSent(Long id, Integer versionId, String brokerMsgId);
}
