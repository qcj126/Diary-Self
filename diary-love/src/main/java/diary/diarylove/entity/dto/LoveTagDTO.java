package diary.diarylove.entity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 创建或更新恋爱记录标签的参数。 */
@Data
public class LoveTagDTO {
    private Long id;
    private Long recordId;
    private Long coupleId;
    private Long creatorUserId;
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 32, message = "标签名称最多32个字符")
    private String tagName;
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "颜色必须是#RRGGBB格式")
    private String color;
    @Min(value = 0, message = "排序值不能小于0")
    private Integer sort;
}
