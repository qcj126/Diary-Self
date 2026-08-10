package diary.common.entity.recipe.dto.req;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecipeCategoryDto {
    // 删除
    private List<Long> categoryIds;

    // 添加
    private String categoryName;
    private Long iconId;

    // 赋值并转换
    private Long categoryId;
    private Long userId;
    private Integer categoryNum;
}
