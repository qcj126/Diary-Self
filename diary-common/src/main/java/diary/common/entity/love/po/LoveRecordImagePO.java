package diary.common.entity.love.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 恋爱记录图片关联持久化对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveRecordImagePO {
    private Long id;
    private Long recordId;
    private Long imageId;
    private Boolean isCover;
    private Integer sort;
    private LocalDateTime createTime;
}
