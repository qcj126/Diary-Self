package diary.common.entity.image.po;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ImagePO {
    private Long id; // 主键
    private Long userId; // 用户ID
    private Integer fileSize; // 文件大小（字节）
    private Boolean deleted; // 是否删除
    private String url; // 图片访问URL
    private String thumbnailUrl; // 缩略图URL
    private String originalName; // 原始文件名
    private String mimeType; // MIME类型，如 image/jpeg
    private Integer type; // 图片类型  1000 正常图，1100 缩略图，1200 裁剪图，2000 未知类型
    private LocalDateTime createdTime; // 上传时间
    private LocalDateTime updatedTime; // 更新时间
}