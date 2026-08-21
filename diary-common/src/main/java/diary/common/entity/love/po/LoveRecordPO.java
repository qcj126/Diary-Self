package diary.common.entity.love.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 恋爱记录持久化对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveRecordPO {
    private Long id;
    private Long coupleId;
    private Long creatorUserId;
    private Long locationId;
    private String title;
    private String content;
    private LocalDate recordDate;
    private String categoryCode;
    private Boolean important;
    private Integer sort;
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
