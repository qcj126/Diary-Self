package diary.file.service.oss;

import diary.file.entity.model.OssCompleteResult;
import diary.file.entity.model.OssObjectMetadata;
import diary.file.entity.model.OssObjectResource;
import diary.file.entity.model.OssUploadedPart;

import java.io.InputStream;
import java.util.List;

public interface OssStorageService {

    /*
    * 校验 objectKey 非空。
    * 校验 contentType 非空。
    * 创建 ObjectMetadata。
    * 设置最终对象的 Content-Type。
    * 调用 OSS 初始化 Multipart。
    * 返回 OSS 生成的 uploadId。
    * */
    String initiateMultipartUpload(
            String objectKey,
            String contentType
    );

    /*
    * 检查必要参数。
    * 检查 partNumber 在 1..10000。
    * 检查 partSize > 0。
    * 构建 UploadPartRequest。
    * 将 InputStream 的指定长度上传到 OSS。
    * 返回 OSS 的 partNumber、ETag、partSize。
    * */
    OssUploadedPart uploadPart(
            String objectKey,
            String ossUploadId,
            int partNumber,
            InputStream inputStream,
            long partSize
    );

    /*
    * 检查列表不为空。
    * 检查 partNumber 范围。
    * 检查 ETag 非空。
    * 检查 partNumber 不重复。
    * 按 partNumber 从小到大排序。
    * 转为阿里云 PartETag。
    * 调用 OSS Complete。
    * Complete 后读取对象元信息。
    * 检查最终对象大小。
    * 处理 NoSuchUpload 的幂等恢复。
    * */
    OssCompleteResult completeMultipartUpload(
            String objectKey,
            String ossUploadId,
            List<OssUploadedPart> parts,
            long expectedFileSize
    );

    /*
    * 初始化时数据库插入失败的补偿逻辑。
    * 并发初始化失败的补偿逻辑。
    * 过期会话清理任务。
    * 用户主动取消上传的后续功能。
    * */
    void abortMultipartUpload(
            String objectKey,
            String ossUploadId
    );

    /*
    * Complete 后验证文件大小。
    * 秒传前确认 OSS 对象仍然存在。
    * 下载和恢复流程。
    * 缩略图生成前获取元信息。
    * */
    OssObjectMetadata getObjectMetadata(
            String objectKey
    );

    /*
    * S2-07 完整文件 SHA-256 验证。
    * S2-08 生成缩略图。
    * 后续 OSS 回源。
    * */
    OssObjectResource openObject(
            String objectKey
    );

    /*
    * Complete 的 NoSuchUpload 恢复。
    * 秒传检查。
    * OSS数据一致性检查。
    * */
    boolean objectExists(
            String objectKey
    );

    /*
    *
    * */
    String putObject(
            String objectKey,
            String contentType,
            InputStream inputStream,
            long contentLength
    );

    /*
    * SHA-256验证失败后的错误对象补偿。
    * 图片软删除后的异步OSS清理。
    * 缩略图失败补偿。
    * */
    void deleteObject(
            String objectKey
    );
}
