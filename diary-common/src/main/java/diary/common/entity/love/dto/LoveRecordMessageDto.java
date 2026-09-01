package diary.common.entity.love.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LoveRecordMessageDto {
    private String clientRequestId;
    private String eventId;
    private Long recordId;
    private Long userId;
    private String taskType;
    private String eventType;
    private String taskStatus;
    private String errorCode;
    private String errorMessage;
    private Integer schemaVersion;
    private LocalDateTime occurTime;
    private String traceId;
}
