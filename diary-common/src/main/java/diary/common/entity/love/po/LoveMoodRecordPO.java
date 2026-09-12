package diary.common.entity.love.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 恋爱记录心情持久化对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveMoodRecordPO {
    private Long id;
    private String moodId;
    private Long recordId;
    private Long coupleId;
    private String moodCode;
    private String moodName;
    private String emoji;
    private Integer sort;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
