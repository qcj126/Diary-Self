package diary.common.entity.timemachine.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 时光机分类PO
 */

@Data
public class TimeCategoryPO {
    private Long id;
    private Long userId;
    private String categoryName;
    private Integer categoryNum;
    private Integer sort;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
