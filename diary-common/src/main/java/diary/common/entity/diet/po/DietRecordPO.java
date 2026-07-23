package diary.common.entity.diet.po;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DietRecordPO {
    /**
     * 主键
     */
    private Long id;

    /**
     * 用户ID（关联用户中心）
     */
    private Long userId;

    /**
     * 食用时间
     */
    private LocalDateTime eatTime;

    /**
     * 餐别：10早餐 15早加餐 20午餐 25午加餐 30晚餐 35夜宵
     */
    private Byte mealType;

    /**
     * 食物/菜品名称
     */
    private String foodName;

    /**
     * 热量（千卡）
     */
    private Integer calories;

    /**
     * 蛋白质(g)
     */
    private BigDecimal protein;

    /**
     * 脂肪(g)
     */
    private BigDecimal fat;

    /**
     * 碳水化合物(g)
     */
    private BigDecimal carbohydrate;

    /**
     * 饱腹感评分 1~10
     */
    private Byte fullnessScore;

    /**
     * 用餐地点（家里/公司/餐厅等）
     */
    private String location;

    /**
     * 备注
     */
    private String note;

    /**
     * 逻辑删除 0正常 1删除
     */
    private Boolean deleted;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
