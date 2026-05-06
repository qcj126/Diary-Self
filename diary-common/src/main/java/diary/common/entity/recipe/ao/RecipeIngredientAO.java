package diary.common.entity.recipe.ao;

import lombok.Data;

@Data
public class RecipeIngredientAO {
    private String name;
    private String quantity;
    private Integer isMain;
    private Integer sortOrder;
}