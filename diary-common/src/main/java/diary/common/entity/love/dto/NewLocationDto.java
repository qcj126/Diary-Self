package diary.common.entity.love.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class NewLocationDto {
    @NotBlank
    @Size(max = 100)
    private String name;

    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String cityCode;
    private String cityName;
}
