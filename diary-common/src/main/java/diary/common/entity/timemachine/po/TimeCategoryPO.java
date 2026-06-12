package diary.common.entity.timemachine.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 时光机分类PO
 */

@Data
public class TimeCategoryPO {
    private Long id;                            // ID
    private Long userId;                        // 用户ID
    private String categoryName;                // 分类名
    private Integer categoryNum;                // 分类编号
    private Integer sort;                       // 排序
    private Integer deleted;                    // 删除标志
    private LocalDateTime createTime;           // 创建时间
    private LocalDateTime updateTime;           // 更新时间
}
