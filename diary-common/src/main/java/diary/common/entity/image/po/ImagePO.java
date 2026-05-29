package diary.common.entity.image.po;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ImagePO {
    /**
     * 主键
     */
    private Long id;

    /**
     * 全局唯一标识（业务层生成）
     */
    @NotNull(message = "图片唯一标识不能为空")
    private String uuid;

    /**
     * 图片访问URL
     */
    @NotNull(message = "图片URL不能为空")
    private String url;

    /**
     * 原始文件名
     */
    @NotNull(message = "原始文件名不能为空")
    private String originalName;

    /**
     * 文件大小（字节）
     */
    @NotNull(message = "文件大小不能为空")
    private Integer fileSize;

    /**
     * MIME类型，如 image/jpeg
     */
    @NotNull(message = "MIME类型不能为空")
    private String mimeType;

    /**
     * 上传时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}