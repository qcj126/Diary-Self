package diary.common.entity.love.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 恋爱足迹地点持久化对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveLocationPO {
    private Long id;
    private Long coupleId;
    private String name;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String cityCode;
    private String cityName;
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
