package diary.common.entity.love.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 恋爱记录标签关联响应对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveRecordTagVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long recordId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tagId;
    private Integer sort;
    private LocalDateTime createTime;
}
