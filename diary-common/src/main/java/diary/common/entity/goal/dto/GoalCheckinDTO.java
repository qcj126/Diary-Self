package diary.common.entity.goal.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Goal check-in create/update request.
 */
@Data
public class GoalCheckinDTO {
    private Long id;
    private String requestId;
    private Long stageGoalId;
    private Long subGoalId;
    private Integer checkinType;
    private BigDecimal spentHours;
    private String content;
    private List<String> evidenceUrls;
    private LocalDate checkinDate;
    private Integer source;
}
