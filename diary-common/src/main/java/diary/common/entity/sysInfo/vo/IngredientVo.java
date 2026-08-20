package diary.common.entity.sysInfo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IngredientVo {
    private Long id;
    private String name;
    private String category;
    private String categoryName;
    private Integer isMain;
    private Long iconId;
    private String iconName;
    private String iconPath;
    private Long userId;
}
