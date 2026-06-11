package diary.common.entity.timemachine.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 时光机分类PO
 */

@Data
public class TimeCategoryPO {
    private Long id;                        // 主键
    private Long userId;                    // 用户ID
    private String categoryName;            // 分类名称
    private Integer categoryNum;            // 分类编号  1100, 1200, 1300...
    private Boolean deleted;                // 是否删除  0：在用 1：删除 默认：0
    private Integer categorySort;           // 分类排序
    private LocalDateTime createdTime;      // 创建时间
    private LocalDateTime updatedTime;      // 更新时间
}
