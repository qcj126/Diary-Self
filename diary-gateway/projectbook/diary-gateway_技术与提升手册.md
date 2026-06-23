# diary-gateway 模块技术与提升手册

## 一、模块概述

diary-gateway 是Diary-Self项目的API网关模块，基于Spring Cloud Gateway实现，提供：
- 路由转发
- JWT认证过滤
- CORS跨域处理
- 负载均衡
- 限流熔断

## 二、技术栈

### 2.1 核心技术
- **Java 21**: 项目基础语言版本
- **Spring Boot 3.5.14**: 基础框架
- **Spring Cloud Gateway Server WebFlux 2025.0.0**: 响应式网关
- **Spring Boot Starter Data Redis Reactive**: 响应式Redis支持
- **JJWT 0.11.5**: JWT令牌解析
- **Lombok 1.18.44**: 简化代码开发

### 2.2 依赖管理
```xml
- spring-cloud-starter-gateway-server-webflux
- spring-boot-starter-data-redis-reactive
- jjwt-api/jjwt-impl/jjwt-jackson
- diary-utils
- diary-common
- spring-boot-starter-actuator
- micrometer-registry-prometheus
```

### 2.3 服务端口
- **端口**: 10000

### 2.4 技术特点
- **响应式编程**: 基于WebFlux的异步非阻塞架构
- **全局过滤器**: JWT认证全局过滤
- **路由配置**: 基于路径的路由转发

## 三、核心功能

### 3.1 路由转发
配置各业务模块的路由规则：
- `/user/**` → diary-user (8801)
- `/file/**` → diary-file (8802)
- `/recipe/**` → diary-recipe (8803)
- `/time-machine/**` → diary-timemachine (8804)
- `/diet/**` → diary-diet (8805)

### 3.2 JWT认证过滤
- **全局过滤器**: JwtAuthFilter
- **白名单路径**: 登录、注册等公开接口
- **Token验证**: 检查Token有效性和黑白名单
- **角色权限**: 验证用户角色
- **请求头传递**: 向下游服务传递用户信息

### 3.3 CORS跨域处理
- 允许来源: `http://localhost:*`
- 允许方法: GET, POST, PUT, DELETE, OPTIONS
- 允许头部: 所有
- 允许凭证: true

### 3.4 监控
- **Actuator**: 健康检查和监控端点
- **Prometheus**: 指标收集和导出

## 四、路由配置

### 4.1 路由表

| 路由ID | 路径谓词 | 目标URI | 端口 |
|--------|---------|---------|------|
| diary-user | /user/** | http://localhost:8801 | 8801 |
| diary-file | /file/** | http://localhost:8802 | 8802 |
| diary-recipe | /recipe/** | http://localhost:8803 | 8803 |
| diary-timemachine | /time-machine/** | http://localhost:8804 | 8804 |
| diary-diet | /diet/** | http://localhost:8805 | 8805 |

### 4.2 白名单路径
```
/user/login
/user/register
/user/verifycode
/user/resetPw
/user/refresh
/actuator
/favicon.ico
```

## 五、优化建议

### 5.1 代码优化
1. **过滤器优化**
   - 建议将白名单配置外部化
   - 增加动态白名单管理
   - 增加过滤器链路日志

2. **路由优化**
   - 建议使用服务发现替代硬编码URI
   - 增加路由动态配置
   - 增加路由健康检查

3. **性能优化**
   - 建议优化Redis查询次数
   - 增加Token缓存
   - 减少不必要的Token解析

### 5.2 架构优化
1. **服务发现集成**
   - 建议集成Nacos或Consul
   - 实现服务自动发现
   - 支持服务动态扩缩容

2. **负载均衡**
   - 建议集成Spring Cloud LoadBalancer
   - 支持多种负载均衡策略
   - 增加健康检查

3. **限流熔断**
   - 建议集成Sentinel或Resilience4j
   - 实现接口级限流
   - 增加熔断降级机制

### 5.3 安全优化
1. **Token安全**
   - 建议增加Token绑定IP
   - 增加Token使用次数限制
   - 增加异常Token检测

2. **防攻击**
   - 增加IP黑白名单
   - 增加请求频率限制
   - 增加恶意请求检测

3. **审计日志**
   - 记录所有网关请求日志
   - 记录认证失败日志
   - 记录异常请求日志

## 六、功能扩展建议

### 6.1 新增功能
1. **API文档聚合**
   - 集成Swagger UI
   - 聚合各服务API文档
   - 提供统一文档入口

2. **灰度发布**
   - 支持灰度路由
   - 支持A/B测试
   - 支持金丝雀发布

3. **协议转换**
   - HTTP转gRPC
   - HTTP转WebSocket
   - 协议适配

### 6.2 监控增强
1. **链路追踪**
   - 集成Sleuth + Zipkin
   - 全链路追踪
   - 性能分析

2. **日志聚合**
   - 集成ELK或Loki
   - 日志集中管理
   - 日志分析告警

3. **指标监控**
   - QPS监控
   - 响应时间监控
   - 错误率监控

## 七、程序风险

### 7.1 高风险
1. **单点故障**
   - 网关是整个系统的入口
   - 建议部署多实例
   - 增加负载均衡器

2. **性能瓶颈**
   - 所有请求都经过网关
   - 网关性能影响整个系统
   - 建议性能压测和优化

3. **Token验证性能**
   - 每次请求都查询Redis
   - 高并发时可能成为瓶颈
   - 建议增加本地缓存

### 7.2 中风险
1. **配置错误**
   - 路由配置错误导致服务不可用
   - 建议增加配置校验
   - 增加配置热更新

2. **依赖风险**
   - 强依赖Redis
   - Redis故障影响认证
   - 建议增加降级机制

3. **响应式编程**
   - WebFlux学习曲线陡峭
   - 调试困难
   - 建议增加测试覆盖

### 7.3 低风险
1. **CORS配置**
   - 当前允许所有localhost端口
   - 生产环境应严格限制

2. **日志级别**
   - 生产环境应调整日志级别
   - 避免过多日志影响性能

## 八、最佳实践

### 8.1 JWT过滤器
```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // 1. 检查是否OPTIONS请求
    // 2. 检查是否在白名单
    // 3. 提取Authorization Header
    // 4. 验证Token
    // 5. 检查Token黑白名单
    // 6. 验证角色权限
    // 7. 传递用户信息到下游
    // 8. 继续过滤器链
}
```

### 8.2 路由配置
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: service-name
          uri: http://localhost:port
          predicates:
            - Path=/service/**
```

### 8.3 CORS配置
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOriginPatterns: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
```

## 九、维护建议

1. **定期性能测试**
   - 压测网关吞吐量
   - 监控响应时间
   - 优化性能瓶颈

2. **安全审计**
   - 检查Token安全策略
   - 检查路由配置
   - 检查CORS配置

3. **监控告警**
   - 配置QPS告警
   - 配置错误率告警
   - 配置响应时间告警

4. **文档维护**
   - 更新路由配置文档
   - 维护过滤器文档
   - 更新部署文档
