package diary.common.entity.diet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DietRecordDTO {

    /**
     * 主键（更新时使用）
     */
    private Long id;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 食用时间
     */
    @NotNull(message = "食用时间不能为空")
    private LocalDateTime eatTime;

    /**
     * 餐别：10早餐 15早加餐 20午餐 25午加餐 30晚餐 35夜宵
     */
    @NotNull(message = "餐别不能为空")
    private Byte mealType;

    /**
     * 食物/菜品名称
     */
    @NotNull(message = "食物名称不能为空")
    @Size(max = 200, message = "食物名称最多200字符")
    private String foodName;

    /**
     * 热量（千卡）
     */
    @NotNull(message = "热量不能为空")
    @Min(value = 0, message = "热量不能为负数")
    private Integer calories;

    /**
     * 蛋白质(g)
     */
    @NotNull(message = "蛋白质不能为空")
    @DecimalMin(value = "0.00", message = "蛋白质不能为负数")
    private BigDecimal protein;

    /**
     * 脂肪(g)
     */
    @NotNull(message = "脂肪不能为空")
    @DecimalMin(value = "0.00", message = "脂肪不能为负数")
    private BigDecimal fat;

    /**
     * 碳水化合物(g)
     */
    @NotNull(message = "碳水化合物不能为空")
    @DecimalMin(value = "0.00", message = "碳水化合物不能为负数")
    private BigDecimal carbohydrate;

    /**
     * 饱腹感评分 1~10
     */
    @Min(value = 1, message = "饱腹感评分最小为1")
    @Max(value = 10, message = "饱腹感评分最大为10")
    private Byte fullnessScore;

    /**
     * 用餐地点
     */
    @Size(max = 100, message = "用餐地点最多100字符")
    private String location;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注最多500字符")
    private String note;
}
