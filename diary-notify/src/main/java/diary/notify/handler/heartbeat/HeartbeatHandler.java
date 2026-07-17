package diary.notify.handler.heartbeat;

import diary.notify.manager.channel.ChannelManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 心跳检测 Handler
 * 配合 IdleStateHandler 实现连接存活检测
 *
 * 工作原理：
 *   1. Pipeline 中前置的 IdleStateHandler 会在指定时间内未收到读事件时触发 IdleStateEvent
 *      例如：IdleStateHandler(60, 0, 0) 表示 60 秒未收到客户端消息则触发
 *   2. 此 Handler 在 userEventTriggered 中捕获 IdleStateEvent
 *   3. 收到读空闲事件后，向客户端发送心跳响应（Pong），或主动关闭连接
 *
 * 心跳策略（两种方案，根据业务选择）：
 *   方案A：服务端主动发送 Pong —— 客户端定时发 Ping，服务端回 Pong
 *   方案B：服务端主动关闭 —— 客户端超时未发消息，服务端直接断开（推荐）
 *
 * Pipeline 位置：
 *   必须在 IdleStateHandler 之后、业务 Handler 之前
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    /**
     * 最大连续未收到心跳次数
     * 超过此次数后，判定连接失效，主动关闭
     */
    private static final int MAX_IDLE_COUNT = 3;

    /**
     * Channel Attribute 中存储空闲计数的 Key
     */
    private static final AttributeKey<Integer> IDLE_COUNT_KEY = AttributeKey.valueOf("idleCount");

    private final ChannelManager channelManager;

    /**
     * 处理用户事件
     * 当 IdleStateHandler 检测到读空闲时，会触发 IdleStateEvent
     *
     * @param ctx ChannelHandlerContext
     * @param evt 事件对象
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                // 从 Channel 的 Attribute 中获取连续空闲次数，不存在则初始化为 0
                Integer idleCount = ctx.channel().attr(IDLE_COUNT_KEY).get();
                if (idleCount == null) {
                    idleCount = 0;
                }
                idleCount++;
    
                if (idleCount >= MAX_IDLE_COUNT) {
                    // 超过阈值，关闭连接并清理资源
                    Long userId = (Long) ctx.channel().attr(AttributeKey.valueOf("username")).get();
                    log.warn("心跳超时，关闭连接: channelId={}, username={}, 连续空闲次数={}",
                            ctx.channel().id(), userId, idleCount);
                    if (userId != null) {
                        channelManager.removeChannel(userId);
                    }
                    ctx.close();
                } else {
                    // 未超过阈值，更新计数并记录日志
                    ctx.channel().attr(IDLE_COUNT_KEY).set(idleCount);
                    log.info("检测到读空闲，当前连续空闲次数: channelId={}, idleCount={}/{}",
                            ctx.channel().id(), idleCount, MAX_IDLE_COUNT);
                }
            }
        }
    }

    /**
     * 异常处理
     * 心跳检测过程中发生异常时，记录日志并关闭连接
     *
     * @param ctx   ChannelHandlerContext
     * @param cause 异常信息
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // TODO: 第一步：记录异常日志，包含连接信息和异常原因
        log.error("心跳检测异常: channel={}, error={}", ctx.channel().id(), cause.getMessage());

        ctx.close();

        super.exceptionCaught(ctx, cause);
    }
}
