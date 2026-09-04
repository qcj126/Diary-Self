package diary.common.entity.love.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 恋爱记录自定义标签持久化对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveTagPO {
    private Long id;
    private Long recordId;
    private Long coupleId;
    private Long creatorUserId;
    private String tagName;
    private String color;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
