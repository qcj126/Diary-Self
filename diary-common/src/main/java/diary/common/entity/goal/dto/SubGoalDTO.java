package diary.common.entity.goal.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubGoalDTO {
    private Long id;
    private Long stageGoalId;
    private Long userId;
    private String title;
    private String content;
    private BigDecimal learnedHours;
    private BigDecimal estimatedHours;
}
