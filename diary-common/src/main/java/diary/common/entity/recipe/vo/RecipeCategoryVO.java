package diary.common.entity.recipe.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class RecipeCategoryVO {
    private Long id;
    private Long userId;
    private String categoryName;
    private String categoryIcon;
    private Integer sort;
}
