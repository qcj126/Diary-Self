package diary.notify.manager.channel;

import io.netty.channel.Channel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Channel 连接管理器
 * 维护 userId ↔ Channel 的映射关系，是消息推送的核心组件
 *
 * 核心职责：
 *   1. 用户连接时，将 userId 与 Channel 绑定
 *   2. 推送消息时，根据 userId 查找对应的 Channel
 *   3. 用户断开时，移除映射关系
 *
 * 线程安全：
 *   使用 ConcurrentHashMap 保证多线程环境下的安全性
 *   （Netty Worker 线程、Spring 业务线程可能同时操作此 Map）
 *
 * 使用场景：
 *   - JwtAuthHandler：认证成功后调用 addChannel() 注册连接
 *   - NotifyHandler：连接关闭时调用 removeChannel() 清理映射
 *   - NotifyMessageConsumer：推送消息时调用 getChannel() 获取连接
 *   - HeartbeatHandler：心跳超时时调用 removeChannel() 清理失效连接
 */
@Slf4j
@Component
@Schema(description = "Channel 连接管理器，维护 userId ↔ Channel 映射关系")
public class ChannelManager {

    /**
     * 用户 Channel 映射表
     * Key：用户ID（Long）
     * Value：对应的 Netty Channel
     *
     * 为什么用 ConcurrentHashMap？
     *   - Netty 的 IO 线程和 Spring 的业务线程可能同时读写此 Map
     *   - ConcurrentHashMap 提供线程安全的读写操作，且性能优于 Hashtable
     */
    @Schema(description = "用户 Channel 映射表，Key 为用户ID，Value 为 Netty Channel")
    private final Map<Long, Channel> userChannels = new ConcurrentHashMap<>();

    /**
     * 添加用户 Channel 映射
     * 当用户认证成功时调用，将 userId 与 Channel 绑定
     *
     * @param userId  用户ID
     * @param channel 用户的 Netty Channel
     */
    public void addChannel(Long userId, Channel channel) {
        // TODO: 第一步：检查是否已存在该用户的连接
        //   - Channel oldChannel = userChannels.get(userId)
        //   - 如果 oldChannel != null 且 oldChannel.isActive()，说明用户重复连接
        //   - 可选择关闭旧连接，保留新连接（单设备登录策略）

        // TODO: 第二步：将新的映射关系存入 ConcurrentHashMap
        //   - userChannels.put(userId, channel)

        // TODO: 第三步：记录日志
        //   - 记录用户ID、Channel ID、当前在线连接数
        log.info("添加Channel映射: userId={}, channelId={}, 当前在线数={}", userId, channel.id(), userChannels.size());
    }

    /**
     * 移除用户 Channel 映射
     * 当用户断开连接或心跳超时时调用
     *
     * @param userId 用户ID
     */
    public void removeChannel(Long userId) {
        // TODO: 第一步：从 Map 中移除映射关系
        //   - Channel removed = userChannels.remove(userId)

        // TODO: 第二步：判断是否成功移除
        //   - 如果 removed != null，记录日志
        //   - 如果 removed == null，说明该用户本就不在线，可能是重复调用

        // TODO: 第三步：记录日志
        //   - 记录用户ID、当前在线连接数
        log.info("移除Channel映射: userId={}, 当前在线数={}", userId, userChannels.size());
    }

    /**
     * 根据用户ID获取 Channel
     * 推送消息时调用，获取目标用户的连接
     *
     * @param userId 用户ID
     * @return 对应的 Channel，如果用户不在线则返回 null
     */
    public Channel getChannel(Long userId) {
        // TODO: 第一步：从 Map 中获取 Channel
        //   - return userChannels.get(userId)
        //   - 返回 null 表示用户不在线，调用方需处理离线逻辑（存入离线消息）
        return userChannels.get(userId);
    }

    /**
     * 判断用户是否在线
     *
     * @param userId 用户ID
     * @return true=在线，false=离线
     */
    public boolean isOnline(Long userId) {
        // TODO: 第一步：检查 Channel 是否存在且处于活跃状态
        //   - Channel channel = userChannels.get(userId)
        //   - return channel != null && channel.isActive()
        return false;
    }

    /**
     * 获取当前在线连接数
     * 用于监控和日志记录
     *
     * @return 当前在线连接数
     */
    public int getOnlineCount() {
        // TODO: 第一步：返回 userChannels.size()
        return userChannels.size();
    }

    /**
     * 获取所有在线用户的 Channel（用于广播场景）
     *
     * @return 所有用户 Channel 的集合
     */
    public Map<Long, Channel> getAllChannels() {
        // TODO: 第一步：返回 userChannels 的不可变视图
        //   - return Collections.unmodifiableMap(userChannels)
        //   - 防止外部直接修改内部 Map
        return userChannels;
    }
}
