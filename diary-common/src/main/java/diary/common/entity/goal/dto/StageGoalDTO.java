package diary.common.entity.goal.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StageGoalDTO {
    private Long id;
    /** 修改目标类型：1 阶段目标，2 小目标。 */
    private Integer flag;
    private Long userId;
    private String creator;
    private String category;
    private String title;
    private String description;
    private Integer status;
    private LocalDateTime ddl;
    private LocalDateTime endTime;
    private List<SubGoalDTO> subGoals;
}
