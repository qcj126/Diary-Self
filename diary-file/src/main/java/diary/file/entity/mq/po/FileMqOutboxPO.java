package diary.file.entity.mq.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** diary-file 图片事件发件箱记录。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMqOutboxPO {
    /** 发件箱记录主键。 */
    private Long id;

    /** 业务事件唯一 ID，重发时保持不变。 */
    private String eventId;

    /** 关联的图片 ID。 */
    private Long imageId;

    /** 业务事件类型。 */
    private String eventType;

    /** RocketMQ Topic。 */
    private String topic;

    /** RocketMQ Tag。 */
    private String tag;

    /** RocketMQ 消息检索 Key。 */
    private String messageKey;

    /** 已序列化的事件 JSON，重发时直接使用。 */
    private String payload;

    /** 消息协议版本。 */
    private Integer schemaVersion;

    /** 发布状态：NEW、SENDING、RETRY_WAIT、SENT 或 DEAD。 */
    private String status;

    /** 额外发送重试次数，不含首次发送。 */
    private Integer retryCount;

    /** 允许的最大额外发送重试次数。 */
    private Integer maxRetries;

    /** 下一次允许领取发送的时间。 */
    private LocalDateTime nextRetryTime;

    /** RocketMQ 返回的消息 ID。 */
    private String brokerMessageId;

    /** 最近一次发送失败或超时的原因。 */
    private String lastError;

    /** 确认消息已发送到 Broker 的时间。 */
    private LocalDateTime sentTime;

    /** 发件箱记录创建时间。 */
    private LocalDateTime createTime;

    /** 记录最近更新时间。 */
    private LocalDateTime updateTime;

    /** 领取与确认发送时使用的乐观锁版本。 */
    private Integer versionId;
}
