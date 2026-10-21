package diary.diaryrecipe.entity.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecipeCategoryVO {
    private Long id;
    private Long userId;
    private String categoryName;
    private Long iconId;
    private String iconPath;
    private Integer sort;
    private Integer categoryNum;
}
