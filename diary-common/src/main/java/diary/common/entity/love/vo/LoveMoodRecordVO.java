package diary.common.entity.love.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 恋爱记录心情响应对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveMoodRecordVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String moodId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long recordId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long coupleId;
    private String moodCode;
    private String moodName;
    private String emoji;
    private Integer sort;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
