package diary.common.entity.image.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ImageDTO {

    /**
     * 全局唯一标识（查询/更新时使用）
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
    @Positive(message = "文件大小必须为正数")
    private Integer fileSize;

    /**
     * MIME类型
     */
    @NotNull(message = "MIME类型不能为空")
    private String mimeType;
}
