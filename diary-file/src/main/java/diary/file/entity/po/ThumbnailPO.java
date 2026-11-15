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
public class ThumbnailPO {

    /** 主键 */
    private Long id;

    /** 关联业务图片ID */
    private Long imageId;

    /** 所属用户 */
    private Long userId;

    /** 文件大小(字节) */
    private Long fileSize;

    /** MIME类型 */
    private String mimeType;

    /** 宽度(px) */
    private Integer width;

    /** 高度(px) */
    private Integer height;

    /** 对象存储key */
    private String objectKey;

    /** 状态: 0-待处理 1-正常 2-失败 */
    private Integer status;

    /** 软删除: 0-否 1-是 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
