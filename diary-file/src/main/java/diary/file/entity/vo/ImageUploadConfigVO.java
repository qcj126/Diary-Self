package diary.file.entity.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImageUploadConfigVO {

    /** 后端要求的分片大小，单位：字节 */
    private Integer chunkSize;

    /** 单张图片最大大小，单位：字节 */
    private Integer maxImageSize;

    /** 允许的MIME类型 */
    private List<String> allowedMimeTypes;

    /** 允许的文件扩展名 */
    private List<String> allowedExtensions;
}