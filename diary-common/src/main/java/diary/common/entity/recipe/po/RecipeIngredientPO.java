package diary.common.entity.recipe.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecipeIngredientPO {
    private Long id;                      // 主键
    private Long recipeId;               // 食谱ID
    private Long userId;                 // 创建者用户ID
    private String name;                   // 食材名称
    private String quantity;               // 食材数量
    private Integer isMain;                 // 是否主料：0-否 1-是
    private Integer sort;                    // 排序
    private Integer deleted;                 // 是否删除：0-否 1-是
    private LocalDateTime createTime;      // 创建时间
    private LocalDateTime updateTime;      // 更新时间
}
