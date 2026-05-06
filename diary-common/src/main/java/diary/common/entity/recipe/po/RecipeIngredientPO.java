package diary.common.entity.recipe.po;

import lombok.Data;

@Data
public class RecipeIngredientPO {
    // 食材表
    /** 食材主键（雪花算法生成） */
    private Long ingredientId;

    /** 食谱ID */
    private Long recipeId;

    /** 食材名称 */
    private String name;

    /** 用量（如：200g、1个） */
    private String quantity;

    /** 是否主料：0-辅料 1-主料 */
    private Integer isMain;

    /** 排序 */
    private Integer sortOrder;
}
