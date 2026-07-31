package diary.common.entity.ai.po;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiInfoPO {
    /**
     * 主键
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * AI模型
     */
    private String model;

    /**
     * AI结果创意度
     */
    private String temperature;

    /**
     * AI类别，对应着AI模型
     */
    private Integer aiType;

    /**
     * AI用途：如鉴别营养成分，每日推荐菜品等
     */
    private Integer aiApplication;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
