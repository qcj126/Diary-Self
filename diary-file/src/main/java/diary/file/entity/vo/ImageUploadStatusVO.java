package diary.file.entity.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageUploadStatusVO {
    /* 图片 ID */
    private Long imageId;

    /* 图片状态编码 */
    private Integer statusCode;

    /* 图片状态枚举名称 */
    private String status;

    /* 面向用户的状态说明 */
    private String description;

    /* 是否已经进入最终状态 */
    private Boolean terminal;

    /* 是否上传成功且可以访问 */
    private Boolean success;
}
