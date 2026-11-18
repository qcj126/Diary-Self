package diary.file.mq.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 图片逻辑删除后，异步清理原图和缩略图OSS对象的领域事件。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageDeletedEvent {
    private String eventId;
    private Integer schemaVersion;
    private String eventType;
    private Long imageId;
    private String originalObjectKey;
    private String thumbnailObjectKey;
    private Long occurredTime;
}
