package diary.notify.server;

import diary.notify.config.NotifyConfig;
import diary.notify.server.initializer.WebSocketChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Netty WebSocket 服务端
 * 负责启动和停止 Netty 服务器，监听 WebSocket 连接
 *
 * 核心职责：
 *   1. 启动 Netty 服务器，绑定监听端口
 *   2. 配置 ChannelInitializer（Pipeline 处理器链）
 *   3. 优雅关闭服务器，释放资源
 *
 * 启动流程：
 *   DiaryNotifyApplication 启动 → 调用 start() → 绑定端口 → 开始接受连接
 *
 * 关闭流程：
 *   应用关闭 → @PreDestroy 触发 → 调用 stop() → 关闭所有连接 → 释放线程组
 *
 * 注意：
 *   - childHandler 在此处设置，引用 WebSocketChannelInitializer
 *   - 避免在 NotifyConfig 中设置 childHandler（防止循环依赖）
 */
@Slf4j
@Component
public class NotifyWebSocketServer {

    @Resource
    private ServerBootstrap serverBootstrap;

    @Resource
    private NotifyConfig notifyConfig;

    @Resource
    private WebSocketChannelInitializer webSocketChannelInitializer;

    private ChannelFuture channelFuture;

    /**
     * 启动 Netty WebSocket 服务器
     * 在应用启动完成后由 ApplicationRunner 调用
     */
    public void start() {
        log.info("Netty WebSocket 服务器启动中...");
        try {
            this.channelFuture = serverBootstrap
                    .childHandler(webSocketChannelInitializer)
                    .bind(notifyConfig.getPort())
                    .sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Netty WebSocket 服务器启动中，端口: {}", notifyConfig.getPort());

        // TODO: 第四步（可选）：添加关闭钩子
        //   - channelFuture.channel().closeFuture().addListener(future -> {
        //       log.info("Netty 服务器已关闭");
        //   })
    }

    /**
     * 停止 Netty WebSocket 服务器
     * 在应用关闭时由 @PreDestroy 方法调用
     */
    public void stop() {
        log.info("Netty WebSocket 服务器正在停止...");
        if (channelFuture == null) {
            return;
        }
        try {
            // 关闭监听端口，不再接受新连接
            channelFuture.channel().close();
            // 阻塞直到所有连接关闭完成
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 释放线程组资源
            serverBootstrap.config().group().shutdownGracefully();
            serverBootstrap.config().childGroup().shutdownGracefully();
        }

        log.info("Netty WebSocket 服务器已停止");
    }
}
