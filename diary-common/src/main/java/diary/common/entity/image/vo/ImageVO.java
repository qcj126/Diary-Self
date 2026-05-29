package diary.common.entity.image.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImageVO {
    /**
     * 全局唯一标识
     */
    private String uuid;

    /**
     * 图片访问URL
     */
    private String url;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文件大小（字节）
     */
    private Integer fileSize;

    /**
     * 格式化后的文件大小（如：1.2 MB）
     */
    private String formattedFileSize;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 文件类型（从MIME类型提取，如：jpeg、png）
     */
    private String fileType;

    /**
     * 上传时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
