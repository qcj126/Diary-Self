package diary.diarydiet.entity.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
     * 用餐地点（家里/公司/餐厅等）
     */
    private String location;

    /**
     * 备注
     */
    private String note;

    /**
     * 食物图片地址
     */
    private String imageUrl;

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
