package diary.common.entity.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskEventDto {
    private String eventId;             // 事件Id
    private String eventType;           // 事件类型
    private Long taskId;                // AI任务Id
    private Long userId;                // 用户Id
    private Long resultId;              // AI任务成功时返回任务Id
    private String errorCode;           // AI任务失败时返回错误码
    private String errorMessage;        // AI任务失败时返回错误信息
    private LocalDateTime occurTime;    // 事件发生时间
    private Integer schemaVersion;      // 事件数据版本
    private String traceId;             // 跟踪Id
}