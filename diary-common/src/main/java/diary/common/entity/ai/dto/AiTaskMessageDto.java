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
public class AiTaskMessageDto {
    private String eventId;
    private Long taskId;
    private Long userId;
    private String taskType;
    private Integer schemaVersion;
    private LocalDateTime occurTime;
    private String traceId;
}
