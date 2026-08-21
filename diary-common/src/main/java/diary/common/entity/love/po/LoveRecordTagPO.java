package diary.common.entity.love.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 恋爱记录标签关联持久化对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveRecordTagPO {
    private Long id;
    private Long recordId;
    private Long tagId;
    private Integer sort;
    private LocalDateTime createTime;
}
