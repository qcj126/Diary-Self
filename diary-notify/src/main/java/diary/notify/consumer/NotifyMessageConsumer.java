package diary.notify.consumer;

import com.rabbitmq.client.Channel;
import diary.notify.manager.channel.ChannelManager;
import diary.notify.manager.offline.OfflineMessageManager;
import diary.notify.protocol.message.NotifyMessage;
import io.netty.channel.ChannelFuture;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * MQ 通知消息消费者
 * 从 RabbitMQ 消费业务事件消息，并推送给在线用户或存储为离线消息
 *
 * 核心职责：
 *   1. 监听 RabbitMQ 通知队列，接收业务模块发送的消息
 *   2. 解析消息，提取目标用户ID和通知内容
 *   3. 检查用户是否在线
 *   4. 在线 → 通过 WebSocket 推送
 *   5. 离线 → 存入离线消息表
 *
 * 消息流转：
 *   业务模块 → notify.exchange → notify.queue → 此消费者 → 推送/存储
 *
 * 注意事项：
 *   - 使用手动 ACK 模式，确保消息可靠消费
 *   - 消费失败时不 ACK，消息会重新入队
 */
@Slf4j
@Component
@Schema(description = "MQ 通知消息消费者，从 RabbitMQ 消费并推送通知")
public class NotifyMessageConsumer {

    @Resource
    private ChannelManager channelManager;

    @Resource
    private OfflineMessageManager offlineMessageManager;

    /**
     * 消费通知消息
     * 监听 notify.queue 队列，接收来自业务模块的通知消息
     *
     * @param message    通知消息对象（由 Jackson2JsonMessageConverter 自动反序列化）
     * @param channel    RabbitMQ Channel（用于手动 ACK）
     * @param deliveryTag 消息投递标签（用于 ACK）
     */
    @RabbitListener(
        queues = "notify.queue",
        ackMode = "MANUAL"
    )
    public void onMessage(NotifyMessage message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // TODO: 第一步：记录接收到的消息日志
            //   - 记录消息类型、通知类型、目标用户ID
            log.info("收到MQ通知消息: type={}, notifyType={}, content={}",
                    message.getType(), message.getNotifyType(), message.getContent());

            // TODO: 第二步：从消息中提取目标用户ID
            //   - Long userId = extractUserId(message)
            //   - 用户ID可能在 message.getExtra() 中，如 extra.get("userId")
            //   - 或者在 message 的其他字段中（需根据实际协议确定）
            Long userId = extractUserId(message);

            // TODO: 第三步：检查用户是否在线
            //   - boolean isOnline = channelManager.isOnline(userId)
            boolean isOnline = channelManager.isOnline(userId);

            // TODO: 第四步：根据用户在线状态决定处理方式
            if (isOnline) {
                // TODO: 第四步（在线）：通过 WebSocket 推送消息
                //   - pushToUser(userId, message)
                pushToUser(userId, message);
            } else {
                // TODO: 第四步（离线）：存储为离线消息
                //   - offlineMessageManager.saveOfflineMessage(userId, message)
                offlineMessageManager.saveOfflineMessage(userId, message);
            }

            // TODO: 第五步：手动 ACK 确认消息已消费
            //   - channel.basicAck(deliveryTag, false)
            //   - false 表示只确认当前消息，不批量确认
            channel.basicAck(deliveryTag, false);

            // TODO: 第六步：记录消费成功日志
            log.info("MQ消息消费成功: notifyType={}, userId={}, 推送方式={}",
                    message.getNotifyType(), userId, isOnline ? "在线推送" : "离线存储");

        } catch (Exception e) {
            // TODO: 第七步：异常处理
            //   - 记录异常日志
            //   - 不 ACK，消息会重新入队等待重试
            //   - 可配置重试次数和死信队列
            log.error("MQ消息消费异常: message={}, error={}", message, e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ex) {
                log.error("NACK失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 推送消息给在线用户
     *
     * @param userId  目标用户ID
     * @param message 通知消息
     */
    private void pushToUser(Long userId, NotifyMessage message) {
        // TODO: 第一步：获取用户的 Channel
        //   - io.netty.channel.Channel nettyChannel = channelManager.getChannel(userId)
        //   - 如果 channel 为 null 或不可用，跳过推送

        // TODO: 第二步：检查 Channel 是否可用
        //   - if (nettyChannel == null || !nettyChannel.isActive()) 则跳过

        // TODO: 第三步：通过 Channel 写入消息
        //   - nettyChannel.writeAndFlush(message)
        //   - NotifyMessageCodec 会自动将 NotifyMessage 编码为 TextWebSocketFrame

        // TODO: 第四步：添加监听器处理发送结果（可选）
        //   - ChannelFuture future = nettyChannel.writeAndFlush(message)
        //   - future.addListener(f -> {
        //       if (f.isSuccess()) { log.info("推送成功"); }
        //       else { log.error("推送失败"); }
        //   })

        // TODO: 第五步：记录推送日志
        log.info("推送消息给用户: userId={}, notifyType={}", userId, message.getNotifyType());
    }

    /**
     * 从消息中提取目标用户ID
     *
     * @param message 通知消息
     * @return 目标用户ID
     */
    private Long extractUserId(NotifyMessage message) {
        // TODO: 第一步：从消息的 extra 字段中提取 userId
        //   - Object userIdObj = message.getExtra().get("userId")
        //   - 转换为 Long 类型并返回
        //   - 如果 extra 为 null 或没有 userId，返回 null 或抛出异常

        // TODO: 第二步：类型转换
        //   - if (userIdObj instanceof Long) return (Long) userIdObj
        //   - if (userIdObj instanceof Integer) return ((Integer) userIdObj).longValue()
        //   - if (userIdObj instanceof String) return Long.parseLong((String) userIdObj)

        return null;
    }
}
