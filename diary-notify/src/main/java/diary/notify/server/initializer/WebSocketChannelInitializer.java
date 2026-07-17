package diary.notify.server.initializer;

import diary.notify.handler.auth.JwtAuthHandler;
import diary.notify.handler.exception.ExceptionHandler;
import diary.notify.handler.heartbeat.HeartbeatHandler;
import diary.notify.handler.notify.NotifyHandler;
import diary.notify.protocol.codec.NotifyMessageCodec;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * WebSocket Channel 初始化器
 * 负责为每个新建立的 WebSocket 连接配置 Pipeline
 *
 * Pipeline 结构（按顺序）：
 *   1. HttpServerCodec          —— HTTP 编解码器（WebSocket 握手基于 HTTP）
 *   2. HttpObjectAggregator     —— HTTP 消息聚合器
 *   3. WebSocketServerProtocolHandler —— WebSocket 协议升级处理器
 *   4. IdleStateHandler         —— 空闲状态检测（心跳基础）
 *   5. JwtAuthHandler           —— JWT 认证（握手后验证身份）
 *   6. HeartbeatHandler         —— 心跳处理（检测连接存活）
 *   7. NotifyMessageCodec       —— 消息编解码（NotifyMessage ↔ TextWebSocketFrame）
 *   8. NotifyHandler            —— 通知消息分发（业务处理）
 *   9. ExceptionHandler         —— 异常处理（最后一道防线）
 *
 * 注意：
 *   - 此类被多个 Channel 共享，所以 Handler 必须使用 @Sharable 注解
 *   - 或者每次 initChannel 时创建新的 Handler 实例（非共享模式）
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class WebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Resource
    private JwtAuthHandler jwtAuthHandler;
    @Resource
    private HeartbeatHandler heartbeatHandler;
    @Resource
    private NotifyHandler notifyHandler;
    @Resource
    private ExceptionHandler exceptionHandler;
    @Resource
    private NotifyMessageCodec notifyMessageCodec;

    /**
     * 初始化新连接的 Pipeline
     * 每当有新的 WebSocket 连接建立时，此方法会被调用
     *
     * @param ch 新建立的 SocketChannel
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        //   - HttpServerCodec：处理 HTTP 请求和响应的编解码
        //   - WebSocket 握手基于 HTTP 协议，必须先添加此 Handler
        pipeline.addLast(new HttpServerCodec());
        //   - HttpObjectAggregator(65536)：将多个 HTTP 消息片段聚合为完整的 FullHttpRequest
        //   - 65536 为最大内容长度（64KB）
        pipeline.addLast(new HttpObjectAggregator(65536));
        //   - WebSocketServerCompressionHandler：启用 permessage-deflate 扩展
        //   - 对 WebSocket 消息进行压缩，减少网络传输量，最大缓冲区为2048字节
        pipeline.addLast(new WebSocketServerCompressionHandler(2048));
        //   - WebSocketServerProtocolHandler("/ws", null, true, 65536)
        //   - "/ws"：WebSocket 路径
        //   - null：子协议（可选）
        //   - true：允许扩展
        //   - 65536：最大帧大小
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true, 65536));
        //   - IdleStateHandler(60, 0, 0, TimeUnit.SECONDS)
        //   - 第一个参数 60：读空闲时间（60秒未收到客户端消息触发）
        //   - 第二个参数 0：写空闲时间（0表示不检测）
        //   - 第三个参数 0：读写空闲时间（0表示不检测）
        pipeline.addLast(new IdleStateHandler(600, 0, 0, TimeUnit.SECONDS));
        //   - jwtAuthHandler：在握手完成后验证用户身份
        //   - 必须放在协议升级之后，业务处理之前
        pipeline.addLast("jwtAuth", jwtAuthHandler);
        //   - heartbeatHandler：处理 IdleStateHandler 触发的空闲事件
        //   - 检测连接存活，清理僵尸连接
        pipeline.addLast("heartbeat", heartbeatHandler);
        //   - notifyMessageCodec：实现 NotifyMessage ↔ TextWebSocketFrame 双向转换
        //   - 编码：NotifyMessage → JSON → TextWebSocketFrame（出站）
        //   - 解码：TextWebSocketFrame → JSON → NotifyMessage（入站）
        pipeline.addLast("codec", notifyMessageCodec);
        //   - notifyHandler：处理业务消息的分发
        pipeline.addLast("notify", notifyHandler);
        //   - exceptionHandler：捕获 Pipeline 中所有前置 Handler 的异常
        //   - 作为最后一道防线，防止异常传播到 Netty 底层
        pipeline.addLast("exception", exceptionHandler);
        log.info("WebSocket Pipeline 初始化完成: channelId={}", ch.id());
    }
}
