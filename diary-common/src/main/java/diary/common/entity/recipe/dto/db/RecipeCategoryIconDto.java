package diary.common.entity.recipe.dto.db;

import lombok.Data;

@Data
public class RecipeCategoryIconDto {
    private Long categoryId;
    private String categoryName;
    private Integer categoryNum;
    private Long iconId;
    private String iconPath;
    private Long userId;
    private Integer sort;
    private String iconName;
    private Integer iconpixel;  // 查询图标时，给图标按照大小分组
}
