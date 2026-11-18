package diary.file.mq.outbox;

import diary.file.entity.po.ImageUploadSessionPO;
import diary.file.mapper.ImageUploadSessionMapper;
import diary.file.property.ImageUploadProperties;
import diary.file.service.filetransactionalservice.ImageTransactionalService;
import diary.file.service.oss.OssStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 回收未完成且已过期的Multipart会话。
 *
 * <p>数据库CAS认领、OSS Abort、数据库清理严格分段执行，避免在事务中进行网络调用。
 * 多实例可同时扫描，只有CAS成功的实例会执行清理；已经进入EXPIRED但上次Abort失败的记录
 * 会在后续周期继续重试。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageUploadSessionCleanupScheduler {
    private final ImageUploadSessionMapper sessionMapper;
    private final ImageUploadProperties properties;
    private final ImageTransactionalService transactionalService;
    private final OssStorageService ossStorageService;

    @Scheduled(fixedDelayString = "${diary.file.image-upload.cleanup-interval-ms:60000}")
    public void cleanupExpiredSessions() {
        List<ImageUploadSessionPO> candidates;
        try {
            candidates = sessionMapper.selectExpiredCleanupCandidates(properties.getCleanupBatchSize());
        } catch (RuntimeException exception) {
            log.error("查询过期图片上传会话失败", exception);
            return;
        }

        for (ImageUploadSessionPO session : candidates) {
            cleanupOne(session);
        }
        cleanupOrphanChunks();
    }

    private void cleanupOne(ImageUploadSessionPO session) {
        try {
            if (!transactionalService.claimExpiredSession(session)) {
                return;
            }
            ossStorageService.abortMultipartUpload(session.getObjectKey(), session.getOssUploadId());
            transactionalService.cleanupExpiredSession(
                    session.getUploadId(), session.getUserId(), session.getImageId()
            );
            log.info("过期图片上传会话清理完成, uploadId={}, imageId={}",
                    session.getUploadId(), session.getImageId());
        } catch (RuntimeException exception) {
            log.warn("过期图片上传会话清理失败，将在后续周期重试, uploadId={}, imageId={}",
                    session.getUploadId(), session.getImageId(), exception);
        }
    }

    private void cleanupOrphanChunks() {
        try {
            int count = transactionalService.cleanupOrphanChunks(properties.getCleanupBatchSize());
            if (count > 0) {
                log.info("孤儿图片分片记录清理完成, count={}", count);
            }
        } catch (RuntimeException exception) {
            log.error("清理孤儿图片分片记录失败", exception);
        }
    }
}
