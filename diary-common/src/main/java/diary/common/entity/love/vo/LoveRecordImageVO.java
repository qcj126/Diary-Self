package diary.common.entity.love.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 恋爱记录图片关联响应对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveRecordImageVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long recordId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long imageId;
    private Boolean isCover;
    private Integer sort;
    private LocalDateTime createTime;
}
