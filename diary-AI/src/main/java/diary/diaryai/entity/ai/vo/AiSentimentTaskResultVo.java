package diary.diaryai.entity.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 恋爱记录情感分析任务的查询结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSentimentTaskResultVo {
    /** AI 任务主键。 */
    private Long taskId;
    /** AI 任务状态。 */
    private String status;
    /** 提交任务时保存的恋爱记录标题。 */
    private String title;
    /** 被分析对象类型，固定为 LOVE_RECORD。 */
    private String targetType;
    /** 被分析恋爱记录主键。 */
    private Long targetId;
    /** AI 应用编码。 */
    private Integer aiApplication;
    /** AI 模型调用信息主键。 */
    private Long aiInfoId;
    /** 情感倾向标签。 */
    private String sentimentLabel;
    /** 情感倾向分数。 */
    private Integer sentimentScore;
    /** AI 分析摘要。 */
    private String analysisSummary;
    /** AI 给出的温和建议。 */
    private String suggestion;
    /** 任务失败错误码。 */
    private String errorCode;
    /** 任务失败错误信息。 */
    private String errorMessage;
    /** 任务创建时间。 */
    private LocalDateTime createTime;
    /** 任务完成时间。 */
    private LocalDateTime finishTime;
}
