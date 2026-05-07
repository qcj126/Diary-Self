package diary.common.entity.recipe.dto.req;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RecipePageReqDto extends PageReqDto {
    // 可选：分类筛选（0-家常 1-西餐 2-甜点 3-汤粥 4-其他）
    private Integer category;

    // 可选：餐别筛选（1-早餐 2-午餐 3-晚餐 4-夜宵）
    private Integer mealType;

    // 可选：难度筛选（1-3）
    private Integer difficulty;

    // 可选：是否纪念日专属（0-否 1-是）
    private Integer isAnniversary;

    // 可选：标题关键词模糊搜索
    private String keyword;
}
