package diary.common.entity.love.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 创建或更新恋爱记录的参数。 */
@Data
public class LoveRecordDTO {
    private Long id;
    @NotNull(message = "恋爱关系ID不能为空")
    private Long coupleId;
    @NotNull(message = "创建者用户ID不能为空")
    private Long creatorUserId;
    private Long locationId;
    @NotBlank(message = "记录标题不能为空")
    @Size(max = 100, message = "记录标题最多100个字符")
    private String title;
    private String content;
    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;
    @NotBlank(message = "分类编码不能为空")
    @Pattern(regexp = "DATE|DAILY|TRAVEL|ANNIVERSARY", message = "分类编码不合法")
    private String categoryCode;
    private Boolean important;
    @Min(value = 0, message = "排序值不能小于0")
    private Integer sort;
}
