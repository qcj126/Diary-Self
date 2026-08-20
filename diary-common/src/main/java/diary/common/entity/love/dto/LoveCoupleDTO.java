package diary.common.entity.love.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 创建或更新恋爱关系的参数。 */
@Data
public class LoveCoupleDTO {
    private Long id;
    @NotNull(message = "创建者用户ID不能为空")
    private Long ownerUserId;
    private Long partnerUserId;
    @NotBlank(message = "伴侣昵称不能为空")
    @Size(max = 32, message = "伴侣昵称最多32个字符")
    private String partnerName;
    @NotNull(message = "恋爱开始日期不能为空")
    private LocalDate startDate;
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Byte status;
}
