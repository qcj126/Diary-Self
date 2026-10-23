package diary.diaryai.entity.ai.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 恋爱记录情感分析结果。
 * 目标对象统一存放在 ai_task.target_type / target_id；本表只保存结果领域字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSentimentPO {
    private Long id;
    private Long userId;
    private Long aiInfoId;
    private Long aiTaskId;
    private String sentimentLabel;
    private Integer sentimentScore;
    private String analysisSummary;
    private String suggestion;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
