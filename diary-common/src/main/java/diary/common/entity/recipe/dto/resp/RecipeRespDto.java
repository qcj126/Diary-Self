package diary.common.entity.recipe.dto.resp;

import diary.common.entity.recipe.po.RecipePO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 食谱响应 DTO（返回给前端的数据）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeRespDto {

    private Long recipeId;
    private String title;
    private String coverImg;
    private String description;
    private Integer category;
    private Integer mealType;
    private Integer difficulty;
    private Integer cookingTime;
    private String story;
    private Integer viewCount;
    private Integer likeCount;
    private Integer cookCount;
    private Integer isAnniversary;
    private LocalDate anniversaryDate;
    private LocalDateTime createdAt;

    /**
     * 实体转 DTO
     */
    public static RecipeRespDto fromEntity(RecipePO recipe) {
        if (recipe == null) return null;
        return RecipeRespDto.builder()
                .recipeId(recipe.getId())
                .title(recipe.getTitle())
                // ... 其他字段
                .build();
    }
}