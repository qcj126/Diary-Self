package diary.common.entity.love.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 保存恋爱记录图片关联的参数。 */
@Data
public class LoveRecordImageDTO {
    private Long id;
    @NotNull(message = "恋爱记录ID不能为空")
    private Long recordId;
    @NotNull(message = "图片ID不能为空")
    private Long imageId;
    private Boolean isCover;
    @Min(value = 0, message = "排序值不能小于0")
    private Integer sort;
}
