package diary.file.property;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "diary.file.rocketmq")
public class FileMqProperties {
    /*mq消息主题*/
    @NotBlank
    private String eventTopic;

    /*图片上传阿里云成功的tag*/
    @NotBlank
    private String imageUploadedTag;

    /*发送消息给此消费组，负责后续将UPLOADED流转到READY*/
    @NotBlank
    private String imageReadyConsumerGroup;

    /*生产者一次从数据库中取得的数据量*/
    @Min(1)
    @NotNull
    private Integer publisherBatchSize;

    /*生产者每隔10秒从数据库中取数据*/
    @Min(100)
    @NotNull
    private Integer publisherIntervalMs;

    /*Outbox 在 SENDING 状态允许停留的最大秒数，超时后进入恢复流程*/
    @Min(1)
    @NotNull
    private Integer outboxSendingTimeoutSeconds;

    /*发生消息失败时，最大的投递次数，不包含首次发送*/
    @Min(0)
    @NotNull
    private Integer outboxMaxRetries;

    @Min(0)
    @NotNull
    private Integer outboxCleanMs;

    /*将outbox状态为sent的消息保留7天，七天后删除*/
    @Min(1)
    @NotNull
    private Integer sentRetentionDays;

    /*清理过期outbox消息的数据量*/
    @Min(1)
    @NotNull
    private Integer cleanBatchSize;
}
