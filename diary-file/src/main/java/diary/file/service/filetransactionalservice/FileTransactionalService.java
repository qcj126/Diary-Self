package diary.file.service.filetransactionalservice;

import diary.file.entity.mq.po.FileMqOutboxPO;

import java.util.List;

public interface FileTransactionalService {
    void recordImageUploaded(Long imageId, String objectKey, String originalName, List<String> failedFiles);

    void recoverTimedOutForRetry(FileMqOutboxPO fileMqOutboxPO);

    boolean claimForPublishing(Long id, Integer versionId);

    boolean markSent(Long id, Integer versionId, String brokerMsgId);
}
