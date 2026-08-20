package diary.common.entity.love.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 创建或更新心情字典的参数。 */
@Data
public class LoveMoodDTO {
    private Long id;
    @NotBlank(message = "心情编码不能为空")
    @Size(max = 32, message = "心情编码最多32个字符")
    private String moodCode;
    @NotBlank(message = "心情名称不能为空")
    @Size(max = 32, message = "心情名称最多32个字符")
    private String moodName;
    @Size(max = 16, message = "Emoji最多16个字符")
    private String emoji;
    @Min(value = 0, message = "排序值不能小于0")
    private Integer sort;
    private Boolean enabled;
}
