package diary.common.entity.sysInfo.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientPo {
    private Long id;
    private String name;
    private String category;
    private String categoryName;
    private Integer isMain;
    private Long iconId;
    private Long userId;
    private String createTime;
    private String updateTime;
}
