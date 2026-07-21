package diary.common.entity.recipe.vo;

import diary.common.entity.recipe.ao.AuthorInfoAO;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecipeVO {
    /** 食谱ID */
    private Long recipeId;

    /** 作者信息 */
    private AuthorInfoAO author;

    /** 标题 */
    private String title;

    /** 封面图 */
    private Long imageId;

    /** 简介 */
    private String description;

    /** 分类 */
    private Integer category;

    /** 分类文本（如"家常"） */
    private String categoryText;

    /** 餐别 */
    private Integer mealType;

    /** 餐别文本（如"晚餐"） */
    private String mealTypeText;

    /** 难度 1-3 */
    private Integer difficulty;

    /** 烹饪时长 */
    private Integer cookingTime;

    /** 情感故事 */
    private String story;

    /** 食材列表 */
    private List<IngredientVO> ingredients;

    /** 步骤列表 */
    private List<StepVO> steps;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
