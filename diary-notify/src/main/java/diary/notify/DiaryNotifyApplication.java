package diary.notify;

import diary.notify.config.NotifyConfig;
import diary.notify.server.NotifyWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;

/**
 * diary-notify 模块启动类
 * 实时通知推送服务，基于 Netty WebSocket 实现长连接通信
 *
 * 核心功能：
 *   1. 启动 Netty WebSocket 服务器，监听客户端连接
 *   2. 消费 RabbitMQ 消息，将业务事件实时推送给前端用户
 *   3. 管理用户在线状态，支持离线消息存储与补推
 *
 * 注解说明：
 *   - @SpringBootApplication：Spring Boot 启动类标识
 *   - @ComponentScan：扫描依赖模块的 Bean
 *     - diary.common：公共实体、枚举
 *     - diary.config：公共配置（Redis、RabbitMQ、MyBatis-Plus 等）
 *     - diary.notify：本模块所有组件
 *     - diary.utils：工具类
 *   - @MapperScan：MyBatis Mapper 接口扫描路径
 *   - @EnableConfigurationProperties：启用配置属性绑定（NotifyConfig）
 *   - @OpenAPIDefinition：Swagger/OpenAPI 文档配置
 */
@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = {"diary.common", "diary.notify", "diary.utils"})
@EnableConfigurationProperties(NotifyConfig.class)

public class DiaryNotifyApplication {

    @Resource
    private NotifyWebSocketServer notifyWebSocketServer;

    public static void main(String[] args) {
        // TODO: 第一步：启动 Spring Boot 应用
        SpringApplication.run(DiaryNotifyApplication.class, args);
        log.info("===== diary-notify 通知服务启动成功 =====");
    }

    /**
     * 应用启动完成后，启动 Netty WebSocket 服务器
     * 使用 ApplicationRunner 确保在 Spring 容器完全初始化后再启动 Netty
     *
     * @return ApplicationRunner
     */
    @Bean
    public ApplicationRunner nettyServerRunner() {
        return args -> {
            // TODO: 第一步：调用 notifyWebSocketServer.start() 启动 Netty 服务器
            //   - 此时所有 Bean 已初始化完成，可以安全注入和使用
            notifyWebSocketServer.start();
            log.info("Netty WebSocket 服务器已通过 ApplicationRunner 启动");
        };
    }

    /**
     * 应用关闭时，优雅关闭 Netty 服务器
     * @PreDestroy 标注的方法会在 Spring 容器销毁前自动调用
     */
    @PreDestroy
    public void destroy() {
        // TODO: 第一步：调用 notifyWebSocketServer.stop() 关闭 Netty 服务器
        //   - 关闭所有连接
        //   - 释放 bossGroup 和 workerGroup 资源
        notifyWebSocketServer.stop();
        log.info("Netty WebSocket 服务器已优雅关闭");
    }
}
