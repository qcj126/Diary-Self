package diary.common.entity.love.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

/** 保存恋爱记录心情数据的参数。 */
@Data
@Builder
public class LoveMoodRecordDTO {
    private Long id;
    @NotBlank(message = "心情ID不能为空")
    private String moodId;
    @NotNull(message = "恋爱记录ID不能为空")
    private Long recordId;
    @NotNull(message = "恋爱关系ID不能为空")
    private Long coupleId;
}
