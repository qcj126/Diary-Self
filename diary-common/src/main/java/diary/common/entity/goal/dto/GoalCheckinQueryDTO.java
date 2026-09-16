package diary.common.entity.goal.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Goal check-in list query. User identity is supplied by the gateway, not by the request body.
 */
@Data
public class GoalCheckinQueryDTO {
    private Long stageGoalId;
    private Long subGoalId;
    private Integer checkinType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer pageNum;
    private Integer pageSize;
}
