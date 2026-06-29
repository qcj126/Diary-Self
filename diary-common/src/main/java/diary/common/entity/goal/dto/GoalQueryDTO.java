package diary.common.entity.goal.dto;

import lombok.Data;

@Data
public class GoalQueryDTO {
    private Long userId;
    private String creator;
    private String category;
    private String title;
    private Integer recentDays;
}
