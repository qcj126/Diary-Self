package diary.file.mq.outbox;

import diary.file.entity.po.ImageUploadSessionPO;
import diary.file.enums.ImageUploadSessionStatusEnum;
import diary.file.mapper.ImageUploadSessionMapper;
import diary.file.property.ImageUploadProperties;
import diary.file.service.filetransactionalservice.ImageTransactionalService;
import diary.file.service.oss.OssStorageService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageUploadSessionCleanupSchedulerTest {
    @Test
    void abortsOssOutsideTransactionsThenCleansDatabase() {
        ImageUploadSessionMapper mapper = mock(ImageUploadSessionMapper.class);
        ImageTransactionalService tx = mock(ImageTransactionalService.class);
        OssStorageService storage = mock(OssStorageService.class);
        ImageUploadProperties properties = new ImageUploadProperties();
        properties.setCleanupBatchSize(20);
        ImageUploadSessionPO session = ImageUploadSessionPO.builder()
                .uploadId("upload-1").imageId(22L).userId(11L)
                .objectKey("images/original/11/22.jpg").ossUploadId("oss-1")
                .status(ImageUploadSessionStatusEnum.UPLOADING.getCode()).version(3).build();
        when(mapper.selectExpiredCleanupCandidates(20)).thenReturn(List.of(session));
        when(tx.claimExpiredSession(session)).thenReturn(true);
        ImageUploadSessionCleanupScheduler scheduler = new ImageUploadSessionCleanupScheduler(
                mapper, properties, tx, storage
        );

        scheduler.cleanupExpiredSessions();

        var order = inOrder(tx, storage);
        order.verify(tx).claimExpiredSession(session);
        order.verify(storage).abortMultipartUpload(session.getObjectKey(), session.getOssUploadId());
        order.verify(tx).cleanupExpiredSession(session.getUploadId(), session.getUserId(), session.getImageId());
        order.verify(tx).cleanupOrphanChunks(20);
    }
}
