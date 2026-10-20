package diary.diarygoal.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubGoalVO {
    private Long id;
    private Long stageGoalId;
    private Long userId;
    private String title;
    private String content;
    private Integer status;
    private BigDecimal learnedHours;
    private BigDecimal estimatedHours;
    private BigDecimal remainingHours;
    private Integer progress;
    private LocalDateTime ddl;
    private LocalDateTime completedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
