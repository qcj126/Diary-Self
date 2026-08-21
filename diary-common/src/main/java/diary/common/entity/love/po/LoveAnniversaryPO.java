package diary.common.entity.love.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 恋爱纪念日持久化对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveAnniversaryPO {
    private Long id;
    private Long coupleId;
    private Long creatorUserId;
    private String name;
    private LocalDate eventDate;
    private Byte repeatType;
    private Integer remindDays;
    private Boolean pinned;
    private Integer sort;
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
