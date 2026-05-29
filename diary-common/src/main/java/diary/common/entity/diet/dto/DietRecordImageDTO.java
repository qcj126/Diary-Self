package diary.common.entity.diet.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DietRecordImageDTO {

    /**
     * 主键（更新时使用）
     */
    private Long id;

    /**
     * 饮食记录ID
     */
    @NotNull(message = "饮食记录ID不能为空")
    private Long recordId;

    /**
     * 图片表ID
     */
    @NotNull(message = "图片ID不能为空")
    private Long imageId;

    /**
     * 图片展示顺序，越小越靠前
     */
    private Integer sortOrder;
}