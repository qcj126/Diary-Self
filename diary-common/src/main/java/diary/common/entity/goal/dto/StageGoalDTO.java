package diary.common.entity.goal.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StageGoalDTO {
    private Long id;
    private Long userId;
    private String creator;
    private String category;
    private String title;
    private String description;
    private LocalDateTime ddl;
    private LocalDateTime endTime;
    private List<SubGoalDTO> subGoals;
}
