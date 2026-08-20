package diary.common.entity.love.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 恋爱纪念日响应对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveAnniversaryVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long coupleId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long creatorUserId;
    private String name;
    private LocalDate eventDate;
    private Byte repeatType;
    private Integer remindDays;
    private Boolean pinned;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
