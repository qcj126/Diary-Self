package diary.common.entity.love.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 创建或更新恋爱纪念日的参数。 */
@Data
public class LoveAnniversaryDTO {
    private Long id;
    @NotNull(message = "恋爱关系ID不能为空")
    private Long coupleId;
    @NotNull(message = "创建者用户ID不能为空")
    private Long creatorUserId;
    @NotBlank(message = "纪念日名称不能为空")
    @Size(max = 64, message = "纪念日名称最多64个字符")
    private String name;
    @NotNull(message = "纪念日日期不能为空")
    private LocalDate eventDate;
    @Min(value = 0, message = "重复类型不合法")
    @Max(value = 1, message = "重复类型不合法")
    private Byte repeatType;
    @Min(value = 0, message = "提前提醒天数不能小于0")
    private Integer remindDays;
    private Boolean pinned;
    @Min(value = 0, message = "排序值不能小于0")
    private Integer sort;
}
