package diary.diaryrecipe.entity.vo;

import lombok.Data;

@Data
public class IngredientVO {
    private Long ingredientId;
    private String name;
    private String quantity;
    private Integer isMain;
}
