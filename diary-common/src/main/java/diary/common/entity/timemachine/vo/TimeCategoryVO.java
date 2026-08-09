package diary.common.entity.timemachine.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 时光机分类PO
 */

@Data
@Builder
public class TimeCategoryVO {
    private Long id;
    private Long userId;
    private String categoryName;
    private Integer sort;
    private String iconName;
    private String iconPath;
}
