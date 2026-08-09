package diary.common.entity.timemachine.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TimeCategoryIconDTO {
    private Long categoryId;                            // ID
    private Long userId;                        // 用户ID
    private Long iconId;                        // 图标ID
    private String categoryName;                // 分类名
    private Integer categoryNum;                // 分类编号
    private Integer sort;                       // 排序
    private Integer deleted;                    // 删除标志
    private LocalDateTime categoryCreateTime;           // 创建时间
    private LocalDateTime categoryUpdateTime;           // 更新时间
    private String iconName;
    private Integer iconType;
    private String iconPath;
    private Integer iconSize;
    private Integer iconPixel;
}
