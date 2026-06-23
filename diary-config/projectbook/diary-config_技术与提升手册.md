# diary-config 模块技术与提升手册

## 一、模块概述

diary-config 是Diary-Self项目的配置中心模块，提供项目中各业务模块共享的配置类，包括：
- MyBatis-Plus配置
- RabbitMQ配置
- OSS配置
- Redis配置
- 线程池配置

## 二、技术栈

### 2.1 核心技术
- **Java 21**: 项目基础语言版本
- **Spring Boot 3.5.14**: 基础框架
- **Spring Boot Starter Web**: Web模块支持
- **Spring Boot Starter Data Redis**: Redis支持
- **Spring Boot Starter AMQP**: RabbitMQ支持
- **MyBatis-Plus Spring Boot3 Starter 3.5.5**: MyBatis-Plus集成
- **Aliyun SDK OSS 3.18.4**: 阿里云OSS存储
- **Lombok 1.18.44**: 简化代码开发

### 2.2 依赖管理
```xml
- spring-boot-starter-web
- spring-boot-starter-data-redis
- spring-data-redis
- mybatis-spring-boot-starter
- spring-boot-starter-amqp
- aliyun-sdk-oss
- mybatis-plus-spring-boot3-starter
- lombok
```

## 三、核心功能

### 3.1 MyBatis-Plus配置（mpconfig）
- 分页插件配置
- 自动填充配置
- 逻辑删除配置
- 性能分析插件

### 3.2 RabbitMQ配置（mqconfig）
- 消息队列连接配置
- 交换机配置
- 队列配置
- 路由键配置
- 消息转换器配置

### 3.3 OSS配置（ossconfig）
- 阿里云OSS客户端配置
- Bucket配置
- 上传策略配置
- 签名URL配置

### 3.4 Redis配置（redisconfig）
- Redis连接池配置
- RedisTemplate配置
- 序列化配置
- 缓存管理器配置

### 3.5 线程池配置（threadpoolconfig）
- 核心线程数配置
- 最大线程数配置
- 队列容量配置
- 拒绝策略配置
- 线程名前缀配置

## 四、优化建议

### 4.1 代码优化
1. **配置类优化**
   - 建议为配置类增加@EnableConfigurationProperties注解
   - 使用@ConfigurationProperties替代@Value，提供更好的类型安全

2. **配置外部化**
   - 建议将配置值移至application.yaml或配置中心
   - 支持不同环境（dev/test/prod）的配置切换

3. **线程池优化**
   - 建议增加线程池监控指标
   - 增加线程池拒绝策略告警

### 4.2 架构优化
1. **模块职责优化**
   - 当前模块依赖了spring-boot-starter-web，但作为配置模块可能不需要
   - 建议仅保留必要的配置依赖

2. **配置分组优化**
   - 建议按业务域分组配置类
   - 增加配置类的激活条件（@ConditionalOnProperty）

### 4.3 性能优化
1. **连接池优化**
   - Redis连接池参数应根据实际负载调优
   - RabbitMQ连接数应限制

2. **缓存优化**
   - 建议配置多级缓存（L1本地缓存+L2 Redis缓存）
   - 增加缓存过期策略

## 五、功能扩展建议

### 5.1 新增配置
1. **数据库连接池配置**
   - Druid/HikariCP配置
   - 多数据源配置
   - 读写分离配置

2. **安全配置**
   - Spring Security配置
   - CORS配置
   - CSRF配置

3. **日志配置**
   - Logback配置
   - 日志级别动态调整
   - 日志脱敏配置

### 5.2 配置中心集成
1. **Nacos配置中心**
   - 配置动态刷新
   - 配置版本管理
   - 配置灰度发布

2. **Apollo配置中心**
   - 命名空间管理
   - 配置回滚
   - 权限控制

## 六、程序风险

### 6.1 高风险
1. **配置泄漏风险**
   - 数据库密码、Redis密码等敏感信息硬编码
   - 建议使用环境变量或配置中心管理敏感信息

2. **线程池配置风险**
   - 不合理的线程池配置可能导致OOM
   - 建议增加线程池参数校验

### 6.2 中风险
1. **版本兼容性**
   - MyBatis-Plus与Spring Boot 3.x版本需匹配
   - Spring Data Redis版本需与Redis服务器版本兼容

2. **连接泄漏**
   - OSS客户端使用后需正确关闭
   - Redis连接需正确释放

### 6.3 低风险
1. **配置重复**
   - 各业务模块可能存在重复配置
   - 建议统一从配置模块引入

2. **配置文档**
   - 配置参数缺少详细说明
   - 建议增加配置参数文档

## 七、最佳实践

### 7.1 配置类设计
1. 使用@Configuration标注配置类
2. 使用@Bean定义Bean
3. 使用@ConditionalOnProperty控制配置激活
4. 使用@ConfigurationProperties绑定配置

### 7.2 线程池配置
```java
@Bean
public ThreadPoolExecutor threadPoolExecutor() {
    return new ThreadPoolExecutor(
        corePoolSize,
        maximumPoolSize,
        keepAliveTime,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(queueCapacity),
        new ThreadFactoryBuilder().setNameFormat("pool-%d").build(),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );
}
```

### 7.3 Redis配置
```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    return template;
}
```

## 八、维护建议

1. **定期审查配置**
   - 检查配置是否过期
   - 检查配置是否合理

2. **配置文档维护**
   - 更新配置参数说明
   - 维护配置示例

3. **配置测试**
   - 增加配置加载测试
   - 增加配置边界测试

4. **安全加固**
   - 敏感信息加密存储
   - 配置访问权限控制
   - 配置变更审计
