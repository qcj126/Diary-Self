package diary.common.entity.recipe.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecipeStepPO {
    private Long id;                      // 主键
    private Long recipeId;               // 食谱ID
    private Long userId;                 // 创建者用户ID
    private Integer stepNumber;           // 步骤编号
    private String description;           // 步骤描述
    private String imageUrl;             // 步骤图片URL
    private Integer timerMinute;          // 步骤计时（分钟）
    private Integer sort;                 // 排序
    private Integer deleted;              // 是否删除：0-否 1-是
    private LocalDateTime createTime;     // 创建时间
    private LocalDateTime updateTime;     // 更新时间
}
