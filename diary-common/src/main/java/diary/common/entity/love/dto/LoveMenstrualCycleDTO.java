package diary.common.entity.love.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 创建或更新生理期记录的参数。 */
@Data
public class LoveMenstrualCycleDTO {
    private Long id;
    @NotNull(message = "恋爱关系ID不能为空")
    private Long coupleId;
    private Long subjectUserId;
    @NotNull(message = "记录者用户ID不能为空")
    private Long recorderUserId;
    @NotNull(message = "生理期开始日期不能为空")
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    @Min(value = 15, message = "周期长度不能小于15天")
    @Max(value = 90, message = "周期长度不能大于90天")
    private Integer cycleLength;
    @Min(value = 1, message = "经期长度不能小于1天")
    @Max(value = 20, message = "经期长度不能大于20天")
    private Integer periodLength;
    private List<@Size(max = 32, message = "单个症状最多32个字符") String> symptoms;
    @Size(max = 500, message = "备注最多500个字符")
    private String note;
    @Min(value = 0, message = "可见范围不合法")
    @Max(value = 1, message = "可见范围不合法")
    private Byte privacyScope;
}
