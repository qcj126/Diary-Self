package diary.common.entity.love.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** 创建或更新恋爱足迹地点的参数。 */
@Data
public class LoveLocationDTO {
    private Long id;
    @NotNull(message = "恋爱关系ID不能为空")
    private Long coupleId;
    @NotBlank(message = "地点名称不能为空")
    @Size(max = 100, message = "地点名称最多100个字符")
    private String name;
    @Size(max = 255, message = "详细地址最多255个字符")
    private String address;
    @DecimalMin(value = "-180", message = "经度不能小于-180")
    @DecimalMax(value = "180", message = "经度不能大于180")
    private BigDecimal longitude;
    @DecimalMin(value = "-90", message = "纬度不能小于-90")
    @DecimalMax(value = "90", message = "纬度不能大于90")
    private BigDecimal latitude;
    @Size(max = 20, message = "城市编码最多20个字符")
    private String cityCode;
    @Size(max = 64, message = "城市名称最多64个字符")
    private String cityName;
}
