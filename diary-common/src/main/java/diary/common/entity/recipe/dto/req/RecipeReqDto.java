package diary.common.entity.recipe.dto.req;

import diary.common.entity.recipe.ao.RecipeIngredientAO;
import diary.common.entity.recipe.ao.RecipeStepAO;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RecipeReqDto {
    /** 创建者用户ID（Controller层从token获取后传入） */
    private Long authorId;

    /** 标题 */
    private String title;

    /** 封面图 */
    private String coverImg;

    /** 简介 */
    private String description;

    /** 分类 */
    private Integer category;

    /** 餐别 */
    private Integer mealType;

    /** 难度 1-3 */
    private Integer difficulty;

    /** 烹饪时长（分钟） */
    private Integer cookingTime;

    /** 情感故事 */
    private String story;

    /** 是否纪念日专属 */
    private Integer isAnniversary;

    /** 纪念日 */
    private LocalDate anniversaryDate;

    /** 状态 0-草稿 1-上架 2-下架 */
    private Integer status;

    /** 食材列表 */
    private List<RecipeIngredientAO> ingredients;

    /** 步骤列表 */
    private List<RecipeStepAO> steps;
}
