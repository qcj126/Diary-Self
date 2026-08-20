package diary.common.entity.love.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 生理期记录响应对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveMenstrualCycleVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long coupleId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long subjectUserId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long recorderUserId;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private Integer cycleLength;
    private Integer periodLength;
    private List<String> symptoms;
    private String note;
    private Byte privacyScope;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
