package diary.common.entity.recipe.po;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RecipePO {

    /**
     * 食谱主键（雪花算法生成）
     */
    private Long recipeId;

    /**
     * 创建者用户ID
     */
    private Long authorId;

    /**
     * 所属情侣空间ID
     */
    private Long coupleId;

    /**
     * 食谱标题
     */
    private String title;

    /**
     * 封面图URL
     */
    private String coverImg;

    /**
     * 简介
     */
    private String description;

    /**
     * 分类：0-家常 1-西餐 2-甜点 3-汤粥 4-其他
     */
    private Integer category;

    /**
     * 餐别：1-早餐 2-午餐 3-晚餐 4-夜宵
     */
    private Integer mealType;

    /**
     * 难度：1-3
     */
    private Integer difficulty;

    /**
     * 烹饪时长（分钟）
     */
    private Integer cookingTime;

    /**
     * 情感故事/备注
     */
    private String story;

    /**
     * 状态：0-草稿 1-已发布 2-已删除
     */
    private Integer status;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 点赞数（冗余）
     */
    private Integer likeCount;

    /**
     * 做过的人数（冗余）
     */
    private Integer cookCount;

    /**
     * 是否纪念日专属：0-否 1-是
     */
    private Integer isAnniversary;

    /**
     * 关联纪念日
     */
    private LocalDate anniversaryDate;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
