package diary.common.entity.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 营养分析结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiNutrientVO {
    /**
     * 关联的业务记录主键。
     */
    private Long universalId;

    /**
     * AI 任务主键。
     */
    private Long aiTaskId;

    /**
     * 热量。
     */
    private String calory;

    /**
     * 蛋白质。
     */
    private String protein;

    /**
     * 脂肪。
     */
    private String fat;

    /**
     * 碳水化合物。
     */
    private String carbohydrate;

    /**
     * 糖。
     */
    private String sugar;

    /**
     * 钠。
     */
    private String sodium;

    /**
     * 分析结果创建时间。
     */
    private LocalDateTime createTime;
}
