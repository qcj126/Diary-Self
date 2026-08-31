package diary.common.entity.love.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 恋爱关系响应对象。 */
@Data
@Builder
public class LoveCoupleVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ownerUserId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long partnerUserId;
    private String partnerName;
    private String startDate;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
