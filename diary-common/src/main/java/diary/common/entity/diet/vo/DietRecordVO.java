package diary.common.entity.diet.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DietRecordVO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 食用时间
     */
    private LocalDateTime eatTime;

    /**
     * 餐别代码
     */
    private Byte mealType;

    /**
     * 餐别名称
     */
    private String mealTypeName;

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
     * 用餐地点
     */
    private String location;

    /**
     * 备注
     */
    private String note;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
