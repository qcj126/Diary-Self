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

    /** 情侣空间ID */
    private Long coupleId;

    /** 标题 */
    private String title;

    /** 封面图 */
    private String coverImg;

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

    /** 是否纪念日专属 */
    private Integer isAnniversary;

    /** 纪念日 */
    private LocalDate anniversaryDate;

    /** 浏览量 */
    private Integer viewCount;

    /** 点赞数 */
    private Integer likeCount;

    /** 做过的人数 */
    private Integer cookCount;

    /** 当前用户是否已点赞 */
    private Boolean isLiked;

    /** 当前用户是否已收藏 */
    private Boolean isFavorited;

    /** 食材列表 */
    private List<IngredientVO> ingredients;

    /** 步骤列表 */
    private List<StepVO> steps;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
