package diary.common.entity.love.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 恋爱记录响应对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveRecordVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long coupleId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long creatorUserId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long locationId;
    private String title;
    private String content;
    private LocalDate recordDate;
    private String categoryCode;
    private Boolean important;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
