package diary.common.entity.timemachine.dto;

import lombok.Data;

import java.util.Date;

/**
 * 时光机分类PO
 */

@Data
public class TimeCategoryDTO {
    private Long id;
    private Long userId;
    private String categoryName;
    private Integer deleted;
    private Integer sort;
}
