package diary.common.entity.mq.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MQ 发件箱实体
 * 用于实现可靠消息最终一致性，保存待发送或已发送的消息记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqOutboxPO {
    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 事件 ID（全局唯一）
     * 用于幂等性校验和消息去重
     */
    private String eventId;

    /**
     * 聚合根类型
     * 如：ORDER、PAYMENT、USER 等
     */
    private String aggregateType;

    /**
     * 聚合根 ID
     * 业务实体的主键 ID
     */
    private Long aggregateId;

    /**
     * 事件类型
     * 如：ORDER_CREATED、ORDER_PAID、USER_REGISTERED 等
     */
    private String eventType;

    /**
     * MQ Topic 名称
     * 消息发送的目标主题
     */
    private String topic;

    /**
     * MQ Tag（RocketMQ 特有）
     * 用于消息分类和过滤，可为空
     */
    private String tag;

    /**
     * 消息 Key
     * 用于消息路由和消费者去重
     */
    private String messageKey;

    /**
     * 消息体（JSON 格式）
     * 已序列化固定下来的消息内容，重发时直接使用此字段，不可重新拼装
     */
    private String payload;

    /**
     * 消息体 Schema 版本
     * 用于兼容性处理，当业务升级时通过版本号识别不同格式
     */
    private Integer schemaVersion;

    /**
     * 消息状态
     * NEW/SENDING/RETRY_WAIT/SENT/DEAD
     */
    private String status;

    /**
     * 当前重试次数
     * 初始值为 0
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     * 超过此次数后状态变为 DEAD
     */
    private Integer maxRetries;

    /**
     * 下次重试时间
     * 用于指数退避延迟重试，为空表示立即重试
     */
    private LocalDateTime nextRetryTime;

    /**
     * MQ 返回的消息 ID
     * 发送成功后由 MQ 服务端返回，用于追踪和查询
     */
    private String brokerMessageId;

    /**
     * 最后一次错误信息
     * 记录发送失败时的异常堆栈或错误描述
     */
    private String lastError;

    /**
     * 消息发送时间
     * 成功发送到 MQ 后记录
     */
    private LocalDateTime sentTime;

    /**
     * 创建时间
     * 记录插入发件箱的时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 每次状态变更或重试时更新
     */
    private LocalDateTime updateTime;

    /**
     * 乐观锁版本号
     * 用于并发更新控制，防止重复消费或重复发送
     */
    private Integer versionId;
}