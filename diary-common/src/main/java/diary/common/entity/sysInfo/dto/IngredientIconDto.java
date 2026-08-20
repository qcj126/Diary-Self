package diary.common.entity.sysInfo.dto;

import lombok.Data;

@Data
public class IngredientIconDto {
    private Long id;
    private String name;
    private String category;
    private String categoryName;
    private Integer isMain;
    private Long iconId;
    private String iconName;
    private String iconPath;
    private Long userId;
    private String createTime;
    private String updateTime;
}
