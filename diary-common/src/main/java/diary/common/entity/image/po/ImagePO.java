package diary.common.entity.image.po;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ImagePO {
    @NotNull
    private Long id;                      // 主键
    @NotNull
    private Long userId;                 // 用户ID
    @NotNull
    private Long fileSize;          // 文件大小（字节）
    @NotNull
    private String objectKey;         // OSS对象键（文件路径，用于生成签名URL）
    @NotNull
    private String originalName;      // 原始文件名
    @NotNull
    private String mimeType;          // MIME类型，如 image/jpeg
    @NotNull
    private Integer type;             // 图片类型 1000 正常图，1100 缩略图，1200 裁剪图，1300 封面图，2000 未知类型
    @NotNull
    private Integer deleted;          // 是否删除：0-否 1-是
    @NotNull
    private Integer status;           // 状态：1200 上传中 1100 失败 1000 成功
    private LocalDateTime createTime;      // 上传时间
    private LocalDateTime updateTime;      // 更新时间
}