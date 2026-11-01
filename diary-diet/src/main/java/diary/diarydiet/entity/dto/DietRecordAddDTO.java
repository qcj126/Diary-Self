package diary.diarydiet.entity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新增饮食记录入参。
 */
@Data
public class DietRecordAddDTO {
    /**
     * 食物或菜品名称。
     */
    @NotNull(message = "食物名称不能为空")
    @Size(max = 200, message = "食物名称最多200字符")
    private String foodName;

    /**
     * 用餐时间。
     */
    @NotNull(message = "用餐时间不能为空")
    private LocalDateTime eatTime;

    /**
     * 餐别：10早餐 15早加餐 20午餐 25午加餐 30晚餐 35夜宵。
     */
    @NotNull(message = "餐别不能为空")
    private Byte mealType;

    /**
     * 用餐地点。
     */
    @NotNull(message = "用餐地点不能为空")
    @Size(max = 100, message = "用餐地点最多100字符")
    private String location;

    /**
     * 可选的食物图片地址。
     */
    @Size(max = 1000, message = "图片地址最多1000字符")
    private String imageUrl;

    /**
     * 可选备注。
     */
    @Size(max = 500, message = "备注最多500字符")
    private String note;
}
