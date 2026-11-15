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
public class ImageUploadChunkPO {

    /** 主键 */
    private Long id;

    /** 上传会话ID */
    private String uploadId;

    /** 分片序号（从0开始） */
    private Integer chunkIndex;

    /** OSS分片编号（从1开始） */
    private Integer partNumber;

    /** 分片SHA-256（ASCII 十六进制字符串） */
    private String chunkSha256;

    /** OSS分片ETag */
    private String etag;

    /** 分片大小(字节) */
    private Integer chunkSize;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
