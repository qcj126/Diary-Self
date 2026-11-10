package diary.file.mq.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.TreeMap;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略新版本消息增加的未知字段，支持向后兼容
/*mq传播的消息实体类，图片成功上传至 OSS 后发布的领域事件*/
public class ImageUploadedEvent {

    /*事件唯一消息id*/
    private String eventId;
    /*消息协议版本*/
    private Integer schemaVersion;
    /*图片mq事件类型*/
    private String eventType;
    /*图片id*/
    private Long imageId;
    /*图片上传阿里云的唯一名称*/
    private String objectKey;
    /*图片的原始名称*/
    private String originalName;
    /*事件发生的时间*/
    private Long occurredTime;
}
