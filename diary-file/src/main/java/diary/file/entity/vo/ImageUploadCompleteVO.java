package diary.file.entity.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageUploadCompleteVO {

    /** 完成上传的图片业务 ID */
    private Long imageId;
}