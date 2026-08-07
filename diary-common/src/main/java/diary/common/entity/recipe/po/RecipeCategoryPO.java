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
    private Integer sort;
    private Long iconId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
