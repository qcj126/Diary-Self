package diary.common.entity.ai.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiNutrientPO {
    /**
     * 主键
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 通用ID
     */
    private Long universalId;

    /**
     * ai结果表主键
     */
    private Long aiInfoId;

    /**
     * 热量 卡路里
     */
    private String calory;

    /**
     * 蛋白质
     */
    private String protein;

    /**
     * 脂肪
     */
    private String fat;

    /**
     * 碳水化合物
     */
    private String carbohydrate;

    /**
     * 糖分
     */
    private String sugar;

    /**
     * 钠含量
     */
    private String sodium;

    /**
     * 标志：饮食或食谱，对应id为universalId
     */
    private String flag;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
