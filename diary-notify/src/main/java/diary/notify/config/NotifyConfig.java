package diary.notify.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "netty")
@Data
public class NotifyConfig {
    private int port;
    private int bossGroupSize;
    private int workerGroupSize;
    private int tcpConnSize;

    @Bean
    public NioEventLoopGroup bossGroup() {
        return new NioEventLoopGroup(bossGroupSize);
    }

    @Bean
    public NioEventLoopGroup workerGroup() {
        return new NioEventLoopGroup(workerGroupSize);
    }

    @Bean
    public ServerBootstrap serverBootstrap() {
        return new ServerBootstrap()
                .group(bossGroup(), workerGroup())
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, tcpConnSize)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);
    }
}
