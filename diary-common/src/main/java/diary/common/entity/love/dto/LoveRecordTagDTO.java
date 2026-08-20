package diary.common.entity.love.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 保存恋爱记录标签关联的参数。 */
@Data
public class LoveRecordTagDTO {
    private Long id;
    @NotNull(message = "恋爱记录ID不能为空")
    private Long recordId;
    @NotNull(message = "标签ID不能为空")
    private Long tagId;
    @Min(value = 0, message = "排序值不能小于0")
    private Integer sort;
}
