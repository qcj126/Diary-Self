package diary.notify.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Netty 服务器配置类
 * 从 application.yml 中读取 netty 前缀的配置项，并创建 Netty 核心组件的 Bean
 *
 * 配置项对应关系（application.yml）：
 *   netty:
 *     port: 8888              → nettyPort（WebSocket 监听端口）
 *     boss-group-size: 2      → bossGroupSize（Boss 线程数，负责接受连接）
 *     worker-group-size: 5    → workerGroupSize（Worker 线程数，负责 IO 处理）
 *     tcp-conn-size: 32       → tcpConnSize（TCP 连接队列大小）
 *
 * 注意：
 *   - childHandler 不在这里设置，由 NotifyWebSocketServer 在启动时设置
 *     因为 childHandler 需要引用 WebSocketChannelInitializer（避免循环依赖）
 */
@Configuration
@ConfigurationProperties(prefix = "netty")
@Data
public class NotifyConfig {

    /**
     * Netty WebSocket 服务监听端口
     * 对应配置：netty.port
     */
    private int port;

    /**
     * Boss 线程组大小
     * Boss 线程负责接受客户端连接（Accept），然后将连接注册到 Worker 线程
     * 通常设置为 1~2 即可（单核即可处理大量连接请求）
     * 对应配置：netty.boss-group-size
     */
    private int bossGroupSize;

    /**
     * Worker 线程组大小
     * Worker 线程负责已连接的 IO 读写处理
     * 默认值为 CPU 核心数 * 2，可根据实际负载调整
     * 对应配置：netty.worker-group-size
     */
    private int workerGroupSize;

    /**
     * TCP 连接队列大小
     * 对应 Netty 的 SO_BACKLOG 选项
     * 表示服务端接受但尚未处理的连接队列长度
     * 对应配置：netty.tcp-conn-size
     */
    private int tcpConnSize;

    /**
     * 创建 Boss 线程组
     * Boss 线程组负责接受客户端连接
     *
     * @return NioEventLoopGroup Boss 线程组
     */
    @Bean
    public NioEventLoopGroup bossGroup() {
        return new NioEventLoopGroup(bossGroupSize);
    }

    /**
     * 创建 Worker 线程组
     * Worker 线程组负责处理已连接的 IO 事件
     *
     * @return NioEventLoopGroup Worker 线程组
     */
    @Bean
    public NioEventLoopGroup workerGroup() {
        return new NioEventLoopGroup(workerGroupSize);
    }

    /**
     * 创建 ServerBootstrap 启动引导类
     * ServerBootstrap 是 Netty 服务端的启动入口，负责组装和启动服务端
     *
     * @return ServerBootstrap 服务端启动引导类
     */
    @Bean
    public ServerBootstrap serverBootstrap() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup(), workerGroup());

        bootstrap.channel(NioServerSocketChannel.class);
        // option用于服务端连接socket的设置
        bootstrap.option(ChannelOption.SO_BACKLOG, tcpConnSize);

        // childOption用于客户端连接Socket的设置
        //   - SO_KEEPALIVE：启用 TCP Keep-Alive 机制，检测死连接
        //   - TCP_NODELAY：禁用 Nagle 算法，减少小包延迟（适合实时推送场景）
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);

        return bootstrap;
    }
}
