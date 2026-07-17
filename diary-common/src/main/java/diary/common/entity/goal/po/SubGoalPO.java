package diary.common.entity.goal.po;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SubGoalPO {
    private Long id;
    private Long stageGoalId;
    private Long userId;
    private String title;
    private String content;
    private BigDecimal learnedHours;
    private BigDecimal estimatedHours;
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
