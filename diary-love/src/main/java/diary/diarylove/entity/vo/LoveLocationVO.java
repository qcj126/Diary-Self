package diary.diarylove.entity.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 恋爱足迹地点响应对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveLocationVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long coupleId;
    private String name;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String cityCode;
    private String cityName;
}
