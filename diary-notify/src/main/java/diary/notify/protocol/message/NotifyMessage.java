package diary.notify.protocol.message;

import diary.notify.enums.NotifyTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 通知消息协议实体
 * 定义 WebSocket 传输的消息格式，是所有通知消息的统一载体
 *
 * 消息流转路径：
 *   业务模块 → MQ → NotifyMessageConsumer → 构建 NotifyMessage → ChannelManager → WebSocket 推送
 *
 * JSON 协议格式示例：
 * {
 *   "type": "NOTIFICATION",
 *   "notifyType": "GOAL_DUE",
 *   "content": "阶段目标'减肥5kg'即将到期！",
 *   "timestamp": 1721136000000,
 *   "extra": { "goalId": 123 }
 * }
 *
 * 字段说明：
 *   - type：消息大类（NOTIFICATION / HEARTBEAT / ACK）
 *   - notifyType：通知子类，对应 NotifyTypeEnum 枚举
 *   - content：通知的文本内容
 *   - timestamp：消息创建时间戳（毫秒）
 *   - extra：扩展数据，承载业务相关的附加信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知消息协议实体，定义 WebSocket 传输的统一消息格式")
public class NotifyMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息类型
     * 可选值：NOTIFICATION（通知）、HEARTBEAT（心跳）、ACK（确认）
     * 用于 NotifyHandler 中的消息分发判断
     */
    @Schema(description = "消息类型：NOTIFICATION / HEARTBEAT / ACK", example = "NOTIFICATION")
    private String type;

    /**
     * 通知子类型
     * 对应 NotifyTypeEnum 枚举的 code 值
     * 用于区分不同的通知业务场景
     */
    @Schema(description = "通知子类型，对应 NotifyTypeEnum", example = "GOAL_DUE")
    private String notifyType;

    /**
     * 通知内容
     * 推送给前端展示的文本内容
     */
    @Schema(description = "通知文本内容", example = "阶段目标'减肥5kg'即将到期！")
    private String content;

    /**
     * 消息时间戳（毫秒级）
     * 消息创建时的系统时间，用于排序和展示
     */
    @Schema(description = "消息时间戳（毫秒）", example = "1721136000000")
    private Long timestamp;

    /**
     * 扩展数据
     * 承载业务相关的附加信息，如目标ID、文件URL等
     * 前端可根据此数据进行页面跳转等操作
     */
    @Schema(description = "扩展数据，承载业务附加信息")
    private Map<String, Object> extra;

    // ========== 消息类型常量 ==========

    /** 通知消息类型 */
    public static final String TYPE_NOTIFICATION = "NOTIFICATION";

    /** 心跳消息类型 */
    public static final String TYPE_HEARTBEAT = "HEARTBEAT";

    /** 确认消息类型 */
    public static final String TYPE_ACK = "ACK";
}
