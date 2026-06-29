package diary.common.entity.goal.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageGoalVO {
    private Long id;
    private Long userId;
    private String creator;
    private String category;
    private String title;
    private String description;
    private BigDecimal learnedHours;
    private BigDecimal estimatedHours;
    private BigDecimal remainingHours;
    private Integer progress;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long daysSinceUpdate;
    private List<SubGoalVO> subGoals;
}
