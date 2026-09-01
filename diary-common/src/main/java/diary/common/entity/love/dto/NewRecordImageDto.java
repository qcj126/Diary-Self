package diary.common.entity.love.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewRecordImageDto {
    @NotNull
    private Long imageId;

    private Boolean isCover;

    @Min(0)
    private Integer sort;
}
