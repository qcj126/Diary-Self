package diary.common.entity.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskStatusVo {
    private Long taskId;
    private String status;
    private Integer attemptCount;
    private Integer maxAttempts;
    private Long resultId;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime queueTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private Integer versionId;
}