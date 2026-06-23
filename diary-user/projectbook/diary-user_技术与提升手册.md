# diary-user 模块技术与提升手册

## 一、模块概述

diary-user 是Diary-Self项目的用户服务模块，提供用户管理相关的功能，包括：
- 用户注册
- 用户登录（密码登录、验证码登录）
- Token管理（生成、刷新、踢出）
- 用户信息管理
- 密码重置

## 二、技术栈

### 2.1 核心技术
- **Java 21**: 项目基础语言版本
- **Spring Boot 3.5.14**: 基础框架
- **Spring Boot Starter Web**: Web模块支持
- **Spring Boot Starter Security**: 安全框架
- **Spring Boot Starter OAuth2 Resource Server**: OAuth2资源服务器
- **Spring Boot Starter Data Redis**: Redis支持
- **MyBatis Spring Boot Starter**: MyBatis集成
- **Druid 1.2.28**: 数据库连接池
- **MySQL Connector J 9.6.0**: MySQL驱动
- **Lombok 1.18.44**: 简化代码开发

### 2.2 依赖管理
```xml
- spring-boot-starter-web
- spring-boot-starter-security
- spring-boot-starter-oauth2-resource-server
- spring-boot-starter-data-redis
- mybatis-spring-boot-starter
- druid-spring-boot-starter
- mysql-connector-j
- diary-utils
- diary-config
- diary-common
- spring-boot-starter-actuator
- micrometer-registry-prometheus
```

### 2.3 服务端口
- **端口**: 8801

## 三、核心功能

### 3.1 用户认证
- **密码登录**: 用户名密码验证
- **验证码登录**: 手机号验证码验证
- **Token管理**: Access Token + Refresh Token双令牌机制
- **Token刷新**: 使用Refresh Token获取新的Token对
- **Token踢出**: 管理员可踢出指定用户

### 3.2 用户管理
- **用户注册**: 新用户注册
- **用户添加**: 管理员添加用户
- **用户删除**: 管理员删除用户
- **用户查询**: 查询用户列表
- **密码重置**: 忘记密码重置

### 3.3 安全机制
- **Spring Security**: 安全框架
- **JWT认证**: 基于JWT的无状态认证
- **角色权限**: 基于角色的访问控制（RBAC）
- **密码加密**: BCrypt加密存储
- **Token黑白名单**: Redis管理Token状态

### 3.4 监控
- **Actuator**: 健康检查和监控端点
- **Prometheus**: 指标收集和导出

## 四、优化建议

### 4.1 代码优化
1. **Service层优化**
   - 建议增加Service接口实现分离
   - 增加事务管理（@Transactional）
   - 增加参数校验

2. **Controller层优化**
   - 建议增加统一的参数校验
   - 增加接口文档（Swagger/OpenAPI）
   - 增加限流机制

3. **Security配置优化**
   - 建议细化Security规则
   - 增加CSRF保护配置
   - 增加CORS配置

### 4.2 架构优化
1. **分层架构**
   - 建议增加Facade层
   - 增加DTO转换层
   - 增加缓存层

2. **数据库优化**
   - 建议增加索引优化
   - 增加查询优化
   - 增加分库分表支持

3. **缓存优化**
   - 建议增加用户信息缓存
   - 增加Token缓存优化
   - 增加缓存预热

### 4.3 性能优化
1. **登录性能**
   - BCrypt加密较慢，建议异步处理
   - 增加登录失败次数限制
   - 增加验证码防刷机制

2. **Token性能**
   - 建议优化Token刷新逻辑
   - 减少Redis查询次数
   - 增加Token批量操作

## 五、功能扩展建议

### 5.1 新增功能
1. **用户资料管理**
   - 头像上传
   - 昵称修改
   - 个人简介

2. **第三方登录**
   - 微信登录
   - QQ登录
   - GitHub登录

3. **安全增强**
   - 两步验证（2FA）
   - 登录设备管理
   - 登录日志记录

4. **用户行为**
   - 登录历史记录
   - 密码修改记录
   - 用户活跃度统计

### 5.2 管理功能
1. **用户管理增强**
   - 用户状态管理（启用/禁用）
   - 用户角色分配
   - 用户批量操作

2. **审计日志**
   - 操作日志记录
   - 登录日志查询
   - 异常行为告警

## 六、程序风险

### 6.1 高风险
1. **密码安全**
   - BCrypt加密强度需合理配置
   - 建议增加密码复杂度校验
   - 定期强制修改密码

2. **Token安全**
   - Access Token有效期较短（10分钟）
   - Refresh Token有效期较长（6小时），存在泄漏风险
   - 建议增加Token绑定设备/IP

3. **SQL注入**
   - MyBatis需使用#{}而非${}
   - 建议增加SQL审计

### 6.2 中风险
1. **并发问题**
   - 用户注册可能存在并发重复
   - 建议增加唯一索引
   - 增加分布式锁

2. **Redis依赖**
   - Token管理强依赖Redis
   - Redis故障会影响认证
   - 建议增加降级机制

3. **验证码安全**
   - 验证码可能被暴力破解
   - 建议增加验证码错误次数限制
   - 增加IP限流

### 6.3 低风险
1. **日志敏感信息**
   - 日志中可能包含用户信息
   - 建议脱敏处理

2. **错误信息暴露**
   - 错误信息可能泄露系统信息
   - 建议统一错误处理

## 七、最佳实践

### 7.1 用户注册
```java
@PostMapping("/register")
public ApiResponse<String> register(@RequestBody UserReqDTO userDTO) {
    // 1. 参数校验
    // 2. 检查用户名是否存在
    // 3. 密码加密
    // 4. 保存用户
    // 5. 返回结果
    return ApiResponse.success(userService.register(userDTO));
}
```

### 7.2 用户登录
```java
@PostMapping("/login")
public ApiResponse<Map<String, Object>> login(@RequestBody UserReqDTO userDTO) {
    // 1. 参数校验
    // 2. 验证用户名密码
    // 3. 生成Token对
    // 4. 保存Token到Redis
    // 5. 返回Token和用户信息
    return ApiResponse.success(userService.login(userDTO));
}
```

### 7.3 Token刷新
```java
@PostMapping("/refresh")
public ApiResponse<TokenPairVO> refresh(@RequestBody RefreshTokenDTO refreshTokenDTO) {
    // 1. 验证Refresh Token
    // 2. 检查Token是否在白名单
    // 3. 生成新的Token对
    // 4. 更新Redis
    // 5. 返回新Token对
    return ApiResponse.success(userService.refresh(refreshTokenDTO.getRefreshToken()));
}
```

## 八、维护建议

1. **定期安全审计**
   - 检查密码加密强度
   - 检查Token安全策略
   - 检查权限控制

2. **性能监控**
   - 监控登录接口响应时间
   - 监控Token刷新成功率
   - 监控Redis命中率

3. **日志管理**
   - 记录登录成功/失败日志
   - 记录Token操作日志
   - 记录异常行为日志

4. **文档维护**
   - 更新API文档
   - 维护安全策略文档
   - 更新部署文档
