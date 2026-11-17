package diary.file.service.uploadservice;

import org.springframework.web.multipart.MultipartFile;

public interface ImageChunkUploadService {
    /**
     * 校验并上传一个图片分片。
     *
     * @param uploadId   对外上传会话ID，不是OSS uploadId
     * @param chunkIndex 分片下标，从0开始
     * @param chunk      当前分片内容
     * @param chunkSha256 前端计算的分片SHA-256
     * @param userId     当前登录用户ID，由服务端认证上下文提供
     */
    void uploadChunk(
            String uploadId,
            int chunkIndex,
            MultipartFile chunk,
            String chunkSha256,
            Long userId
    );
}
