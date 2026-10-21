package diary.diaryrecipe.entity.ao;

import lombok.Data;

@Data
public class RecipeStepAO {
    private Integer stepNumber;
    private String description;
    private Integer timerMin;
    private Integer sort;
}