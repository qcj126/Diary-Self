package diary.common.entity.recipe.po;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RecipePO {
    @NotNull
    private Long id;                      // 主键
    @NotNull
    private Long userId;                 // 创建者用户ID
    @NotNull
    private String title;                   // 标题
    @NotNull
    private String coverImg;               // 封面图URL
    @NotNull
    private String description;           // 简介
    @NotNull
    private Integer category;            // 分类：0-家常 1-西餐 2-甜点 3-汤粥 4-其他
    @NotNull
    private Integer mealType;            // 餐别：1-早餐 2-午餐 3-晚餐 4-夜宵
    @NotNull
    private Integer difficulty;          // 难度：1-5
    @NotNull
    private Integer cookingTime;       // 烹饪时长（分钟）
    private String story;                   // 情感故事/备注
    @NotNull
    private Integer sort;                    // 排序
    @NotNull
    private Integer deleted;                 // 是否删除：0-否 1-是
    @NotNull
    private LocalDateTime createTime;      // 创建时间
    @NotNull
    private LocalDateTime updateTime;      // 更新时间
}
