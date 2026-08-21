package diary.common.entity.love.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 恋爱关系持久化对象。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveCouplePO {
    private Long id;
    private Long ownerUserId;
    private Long partnerUserId;
    private String partnerName;
    private String startDate;
    private Byte status;
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
