package diary.common.entity.love.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewLoveTag {
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 32, message = "标签名称最多32个字符")
    private String tagName;
    @NotBlank(message = "标签颜色不能为空")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "颜色必须是#RRGGBB格式")
    private String color;
    @Min(value = 0, message = "排序值不能小于0")
    private Integer sort;
}
