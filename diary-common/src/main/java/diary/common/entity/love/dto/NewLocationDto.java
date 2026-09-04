package diary.common.entity.love.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
    @NotBlank
    @Size(max = 255)
    private String address;
    @DecimalMin("-180")
    @DecimalMax("180")
    private BigDecimal longitude;
    @DecimalMin("-90")
    @DecimalMax("90")
    private BigDecimal latitude;
    @NotBlank
    @Size(max = 20)
    private String cityCode;
    @NotBlank
    @Size(max = 64)
    private String cityName;
}
