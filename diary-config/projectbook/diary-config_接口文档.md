# diary-config 模块接口文档

## 模块说明

diary-config 是配置中心模块，**不提供独立的HTTP接口**。

本模块主要为其他业务模块提供：
- MyBatis-Plus配置
- RabbitMQ配置
- OSS配置
- Redis配置
- 线程池配置

## 配置说明

### 应用配置（application.yaml）

```yaml
spring:
  application:
    name: diary-config
```

## 配置类说明

### 1. MyBatis-Plus配置

**配置类**: `MpConfig`

**功能**:
- 分页插件配置
- 自动填充配置
- 逻辑删除配置

**使用方式**:
```java
@Autowired
private MpConfig mpConfig;
```

### 2. RabbitMQ配置

**配置类**: `MqConfig`

**功能**:
- 消息队列连接配置
- 交换机配置
- 队列配置

**使用方式**:
```java
@Autowired
private RabbitTemplate rabbitTemplate;
```

### 3. OSS配置

**配置类**: `OssConfig`

**功能**:
- 阿里云OSS客户端配置
- Bucket配置

**配置参数**:
```yaml
aliyun:
  oss:
    endpoint: oss-cn-chengdu.aliyuncs.com
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    bucket-name: your-bucket-name
    region: cn-chengdu
```

**使用方式**:
```java
@Autowired
private OSS ossClient;
```

### 4. Redis配置

**配置类**: `RedisConfig`

**功能**:
- RedisTemplate配置
- 序列化配置

**配置参数**:
```yaml
spring:
  data:
    redis:
      host: 192.168.101.128
      port: 6379
      password: your-password
```

**使用方式**:
```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;
```

### 5. 线程池配置

**配置类**: `ThreadPoolConfig`

**功能**:
- 异步任务线程池
- 线程池参数配置

**使用方式**:
```java
@Autowired
@Qualifier("taskExecutor")
private ThreadPoolExecutor taskExecutor;
```

## 配置项说明

### Redis配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| spring.data.redis.host | String | localhost | Redis服务器地址 |
| spring.data.redis.port | Integer | 6379 | Redis服务器端口 |
| spring.data.redis.password | String | - | Redis密码 |

### RabbitMQ配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| spring.rabbitmq.host | String | localhost | RabbitMQ服务器地址 |
| spring.rabbitmq.port | Integer | 5672 | RabbitMQ服务器端口 |
| spring.rabbitmq.username | String | guest | RabbitMQ用户名 |
| spring.rabbitmq.password | String | guest | RabbitMQ密码 |

### OSS配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| aliyun.oss.endpoint | String | - | OSS Endpoint |
| aliyun.oss.access-key-id | String | - | AccessKey ID |
| aliyun.oss.access-key-secret | String | - | AccessKey Secret |
| aliyun.oss.bucket-name | String | - | Bucket名称 |
| aliyun.oss.region | String | - | 区域 |

### 线程池配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| threadpool.core-size | Integer | 10 | 核心线程数 |
| threadpool.max-size | Integer | 20 | 最大线程数 |
| threadpool.queue-capacity | Integer | 100 | 队列容量 |
| threadpool.keep-alive-time | Long | 60 | 线程存活时间（秒） |

## 使用示例

### 1. 使用Redis

```java
@Service
public class RedisService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }
    
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
```

### 2. 使用RabbitMQ

```java
@Service
public class MqProducer {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendMessage(String exchange, String routingKey, Object message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
```

### 3. 使用OSS

```java
@Service
public class OssService {
    
    @Autowired
    private OSS ossClient;
    
    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;
    
    public String uploadFile(String objectName, InputStream inputStream) {
        ossClient.putObject(bucketName, objectName, inputStream);
        return "https://" + bucketName + ".oss-cn-chengdu.aliyuncs.com/" + objectName;
    }
}
```

### 4. 使用线程池

```java
@Service
public class AsyncService {
    
    @Autowired
    @Qualifier("taskExecutor")
    private ThreadPoolExecutor taskExecutor;
    
    public void asyncTask() {
        taskExecutor.submit(() -> {
            // 异步任务逻辑
        });
    }
}
```

## 注意事项

1. **敏感信息管理**
   - 生产环境的密码等敏感信息应使用环境变量或配置中心管理
   - 不要将敏感信息提交到代码仓库

2. **连接池配置**
   - 根据实际负载调整连接池参数
   - 监控连接池使用情况

3. **线程池配置**
   - 合理设置线程池大小
   - 配置拒绝策略防止OOM

4. **配置刷新**
   - 部分配置修改后需要重启应用
   - 建议集成配置中心实现动态刷新
