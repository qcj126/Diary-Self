package diary.common.entity.love.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("love_location")
public class LoveLocationPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long coupleId;
    private String name;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String cityCode;
    private String cityName;
    @TableLogic
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
