package diary.common.entity.recipe.po;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecipeCategoryPO {
    private Long id;
    private Long userId;
    private String categoryName;
    private Integer categoryNum;
    private String categoryIcon;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
