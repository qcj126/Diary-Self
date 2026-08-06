package diary.common.entity.recipe.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecipeCategoryDto {
    // 删除
    private List<Long> ids;

    // 添加
    private String categoryName;
    private String categoryIcon;

    // 赋值并转换
    private Long id;
    private Long userId;
    private Integer categoryNum;
}
