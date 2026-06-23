# diary-utils 模块技术与提升手册

## 一、模块概述

diary-utils 是Diary-Self项目的工具类模块，提供项目中各业务模块共享的工具类，包括：
- JWT工具类
- OSS工具类
- 加密工具类
- 文件工具类
- Redis工具类
- 雪花算法工具类
- 校验工具类
- 通用工具类

## 二、技术栈

### 2.1 核心技术
- **Java 21**: 项目基础语言版本
- **Spring Boot 3.5.14**: 基础框架
- **Spring Boot Starter Web**: Web模块支持
- **Spring Boot Starter Data Redis**: Redis支持
- **JJWT 0.11.5**: JWT令牌生成与解析
- **Aliyun SDK OSS 3.18.4**: 阿里云OSS存储
- **jBCrypt 0.4**: 密码加密
- **Lombok 1.18.44**: 简化代码开发

### 2.2 依赖管理
```xml
- spring-boot-starter-web
- spring-boot-starter-data-redis
- jjwt-api/jjwt-impl/jjwt-jackson
- aliyun-sdk-oss
- jbcrypt
- lombok
- diary-common
```

## 三、核心功能

### 3.1 JWT工具类（JwtUtil）
- 生成Access Token（有效期10分钟）
- 生成Refresh Token（有效期6小时）
- 解析Token获取用户名
- 解析Token获取角色信息
- 解析Token获取Token ID
- 验证Token类型（Access/Refresh）

### 3.2 OSS工具类（OssUtil）
- 生成OSS签名URL
- 从OSS URL中提取Object Key
- 根据文件名生成签名URL
- 签名URL有效期5分钟

### 3.3 加密工具类（BCryptUtilNoSpring）
- BCrypt密码加密
- BCrypt密码验证
- 不依赖Spring的独立实现

### 3.4 文件工具类（FileUtil）
- 文件下载
- 文件保存
- 文件路径处理

### 3.5 Redis工具类（ValidatUtil）
- 验证码存储
- 验证码验证
- 验证码过期管理

### 3.6 雪花算法工具类（SnowflakeIdUtil）
- 分布式唯一ID生成
- 雪花算法实现
- 高并发ID生成

### 3.7 校验工具类
- **EmailValidationUtil**: 邮箱格式校验
- **PhoneValidationUtil**: 手机号格式校验

### 3.8 通用工具类（MyUtils）
- 主键生成（雪花算法）
- 字符串空值判断
- 对象空值判断
- 文件空值判断
- 对象与JSON互转
- 对象转字符串

## 四、优化建议

### 4.1 代码优化
1. **JWT工具类优化**
   - 建议增加Token刷新机制
   - 增加Token黑名单检查
   - 增加Token续期功能

2. **OSS工具类优化**
   - 建议增加文件上传功能
   - 增加文件删除功能
   - 增加批量操作支持

3. **通用工具类优化**
   - 建议使用Apache Commons Lang替代自定义工具类
   - 增加更多实用工具方法

### 4.2 架构优化
1. **工具类设计**
   - 建议将静态工具类改为Spring Bean
   - 支持依赖注入和单元测试
   - 增加接口抽象便于Mock

2. **配置外部化**
   - JWT密钥应从环境变量读取
   - OSS配置应支持多环境
   - 增加配置校验

### 4.3 性能优化
1. **雪花算法优化**
   - 建议增加时钟回拨处理
   - 增加Worker ID自动分配
   - 增加ID生成监控

2. **JSON转换优化**
   - ObjectMapper应复用，不要每次创建
   - 增加JSON序列化配置
   - 增加缓存机制

## 五、功能扩展建议

### 5.1 新增工具类
1. **日期工具类**
   - 日期格式化
   - 日期计算
   - 时区转换

2. **字符串工具类**
   - 字符串截取
   - 字符串脱敏
   - 字符串编码转换

3. **集合工具类**
   - 集合判空
   - 集合转换
   - 集合分页

4. **HTTP工具类**
   - HTTP请求封装
   - 文件上传下载
   - 超时配置

### 5.2 安全增强
1. **加密工具增强**
   - 增加AES加密解密
   - 增加RSA加密解密
   - 增加MD5/SHA哈希

2. **签名工具**
   - 增加HMAC签名
   - 增加数字签名
   - 增加签名验证

## 六、程序风险

### 6.1 高风险
1. **JWT密钥泄漏**
   - 当前JWT密钥硬编码在配置中
   - 建议使用环境变量或密钥管理服务
   - 定期轮换密钥

2. **OSS凭证泄漏**
   - AccessKey ID和Secret硬编码
   - 建议使用RAM角色或STS临时凭证
   - 限制Bucket权限

### 6.2 中风险
1. **雪花算法时钟回拨**
   - 系统时钟回拨可能导致ID重复
   - 建议增加时钟回拨检测和补偿机制

2. **BCrypt性能**
   - BCrypt加密较慢（约300ms/次）
   - 建议异步处理或降低rounds

3. **JSON转换异常**
   - 当前捕获异常后返回null
   - 建议抛出自定义异常

### 6.3 低风险
1. **工具类命名**
   - MyUtils命名不够明确
   - 建议按功能拆分

2. **日志级别**
   - 部分日志使用debug级别
   - 生产环境可能看不到关键日志

## 七、最佳实践

### 7.1 JWT使用
```java
@Autowired
private JwtUtil jwtUtil;

// 生成Token
String accessToken = jwtUtil.generateAccessToken(username, roles);
String refreshToken = jwtUtil.generateRefreshToken(username, roles);

// 解析Token
String username = jwtUtil.extractUsername(token);
List<String> roles = jwtUtil.extractRoles(token);
```

### 7.2 OSS使用
```java
@Autowired
private OssUtil ossUtil;

// 生成签名URL
String signedUrl = ossUtil.generateSignedUrlByKey(objectKey);
```

### 7.3 雪花算法使用
```java
// 生成分布式ID
long id = SnowflakeIdUtil.nextId();
// 或使用工具类
long id = MyUtils.getPrimaryKey();
```

### 7.4 密码加密
```java
@Autowired
private BCryptUtilNoSpring bcryptUtil;

// 加密密码
String encryptedPassword = bcryptUtil.encrypt(rawPassword);

// 验证密码
boolean matches = bcryptUtil.matches(rawPassword, encryptedPassword);
```

## 八、维护建议

1. **定期更新依赖**
   - JJWT最新版本
   - Aliyun OSS SDK最新版本
   - jBCrypt最新版本

2. **增加单元测试**
   - JWT生成和解析测试
   - OSS签名URL测试
   - 加密解密测试
   - 雪花算法ID唯一性测试

3. **安全加固**
   - 敏感信息使用环境变量
   - 增加密钥轮换机制
   - 定期审计工具类安全性

4. **文档维护**
   - 更新工具类使用示例
   - 维护API文档
   - 记录已知问题和解决方案
