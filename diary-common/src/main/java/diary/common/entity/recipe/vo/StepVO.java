package diary.common.entity.recipe.vo;

import lombok.Data;

@Data
public class StepVO {
    private Long stepId;
    private Integer stepNumber;
    private String description;
    private Integer timerMin;
}
