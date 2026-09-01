package diary.common.entity.love.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewTagDto {
    @NotBlank
    @Size(max = 32)
    private String tagName;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    private String color;
}
