package diary.common.entity.love.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 创建或更新恋爱关系的参数。 */
@Data
public class LoveCoupleDTO {
    private Long id;
    @NotNull(message = "创建者用户ID不能为空")
    private Long ownerUserId;
    private Long partnerUserId;
    private String partnerName;
    @NotNull(message = "恋爱开始日期不能为空")
    private String startDate;
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Integer status;
}
