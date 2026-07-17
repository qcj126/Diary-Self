package diary.notify.server.initializer;

import diary.notify.handler.auth.JwtAuthHandler;
import diary.notify.handler.exception.ExceptionHandler;
import diary.notify.handler.heartbeat.HeartbeatHandler;
import diary.notify.handler.notify.NotifyHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import jakarta.annotation.Resource;

public class WebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Resource
    private JwtAuthHandler jwtAuthHandler;
    @Resource
    private HeartbeatHandler heartbeatHandler;
    @Resource
    private NotifyHandler notifyHandler;
    @Resource
    private ExceptionHandler exceptionHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // HTTP编解码器
        pipeline.addLast(new HttpServerCodec());
        // 聚合HTTP请求
        pipeline.addLast(new HttpObjectAggregator(65536));
        // WebSocket协议升级
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true, 65536));

        // 自定义Handler - 顺序很重要
        pipeline.addLast("jwtAuth", jwtAuthHandler);
        pipeline.addLast("heartbeat", heartbeatHandler);
        pipeline.addLast("notify", notifyHandler);
        pipeline.addLast("exception", exceptionHandler);
    }
}
