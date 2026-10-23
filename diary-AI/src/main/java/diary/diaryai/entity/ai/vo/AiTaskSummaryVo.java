package diary.diaryai.entity.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 任务列表摘要。
 *
 * <p>只包含所有任务类型共享的字段，不关联任何具体结果表。
 * 具体分析结果应根据 {@code taskType} 调用对应的结果查询接口获取。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskSummaryVo {
    private Long taskId;
    private String taskType;
    private String status;
    private String title;
    private String targetType;
    private Long targetId;
    private Integer aiApplication;
    private Integer attemptCount;
    private Integer maxAttempts;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime queueTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
}
