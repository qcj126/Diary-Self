package diary.common.entity.love.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 恋爱记录自定义标签响应对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveTagVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long coupleId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long creatorUserId;
    private String tagName;
    private String color;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
