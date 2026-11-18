package diary.file.service.filetransactionalservice;

import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ImageUploadSessionPO;

public interface ImageTransactionalService {
    void initSessionAndImage(ImagePO imagePO, ImageUploadSessionPO sessionPO);

    void completeMultipartUpload(String uploadId, Long userId, Long imageId);

    void failMultipartUpload(String uploadId, Long userId, Long imageId);

    boolean claimExpiredSession(ImageUploadSessionPO session);

    void cleanupExpiredSession(String uploadId, Long userId, Long imageId);

    int cleanupOrphanChunks(int limit);
}
