package diary.file.entity.po;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ImagePO {

    /** 主键 */
    @NotNull
    private Long id;

    /** 用户ID */
    @NotNull
    private Long userId;

    /** 文件大小（字节） */
    @NotNull
    private Long fileSize;

    /** OSS对象键（文件路径，用于生成签名URL） */
    @NotNull
    private String objectKey;

    /** 原始文件名 */
    @NotNull
    private String originalName;

    /** MIME类型，如 image/jpeg */
    @NotNull
    private String mimeType;

    /** 图片所属业务分类，编码定义见 ImageTypeEnum */
    @NotNull
    private Integer type;

    /** 是否删除：0-否 1-是 */
    @NotNull
    private Integer deleted;

    /** 状态：1200-上传中 1100-失败 1000-成功 */
    @NotNull
    private Integer status;

    /** SHA-256 hash值（ASCII 十六进制字符串） */
    private String sha256;

    /** 图片宽度(px) */
    private Integer width;

    /** 图片高度(px) */
    private Integer height;

    /** 上传时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
