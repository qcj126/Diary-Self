package diary.common.entity.recipe.po;

import lombok.Data;

@Data
public class RecipeStepPO {
    /** 步骤主键（雪花算法生成） */
    private Long stepId;

    /** 食谱ID */
    private Long recipeId;

    /** 步骤编号 */
    private Integer stepNumber;

    /** 步骤文字描述 */
    private String description;

    /** 步骤图片URL */
    private String imageUrl;

    /** 该步骤计时（分钟），NULL 表示无需计时 */
    private Integer timerMin;

    /** 排序 */
    private Integer sortOrder;
}
