package diary.recipe.entity.vo;

import lombok.Data;

@Data
public class StepVO {
    private Long stepId;
    private Integer stepNumber;
    private String description;
    private Integer timerMin;
}
