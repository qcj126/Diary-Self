package diary.file.entity.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadSessionPO {

    /** 上传会话ID(主键) */
    private String uploadId;

    /** 业务图片ID */
    private Long imageId;

    /** 所属用户 */
    private Long userId;

    /** OSS分片上传的UploadId */
    private String ossUploadId;

    /** 对象存储key */
    private String objectKey;

    /** 整文件SHA256(hex) */
    private String sha256;

    /** 文件总大小(字节) */
    private Long fileSize;

    /** 分片大小(字节) */
    private Integer chunkSize;

    /** 分片总数 */
    private Integer totalChunks;

    /** 状态: 0-初始化 1-上传中 2-合并中 3-完成 4-已中止 5-过期 */
    private Integer status;

    /** 乐观锁版本号 */
    private Integer version;

    /** 会话过期时间 */
    private LocalDateTime expiresAt;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
