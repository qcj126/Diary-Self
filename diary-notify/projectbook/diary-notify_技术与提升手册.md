# diary-notify 技术与提升手册

## 一、模块概述

### 1.1 模块定位
`diary-notify` 是 Diary-Self 项目中的**实时通知推送服务模块**，基于 Netty WebSocket 实现长连接通信，负责将系统中的各类事件实时推送给前端用户。

### 1.2 设计目标
- 提供低延迟、高并发的实时消息推送能力
- 支持多种通知类型（目标提醒、饮食提醒、任务完成通知等）
- 支持用户在线状态管理与离线消息补推
- 与现有微服务架构解耦，通过 MQ 异步接收业务事件

### 1.3 核心职责
| 职责 | 说明 |
|------|------|
| 长连接管理 | 维护 WebSocket 连接，管理用户 Channel 映射 |
| 消息推送 | 将业务事件实时推送给在线用户 |
| 心跳检测 | 检测连接存活状态，清理失效连接 |
| 认证鉴权 | 基于 JWT Token 验证用户身份 |
| 离线消息 | 存储离线消息，用户上线后补推 |

---

## 二、核心技术组件

### 2.1 Netty 的作用

#### 为什么选择 Netty？
Netty 是一个高性能的异步事件驱动网络应用框架，在本模块中承担以下核心职责：

| 能力 | 说明 |
|------|------|
| **高并发连接** | 基于 NIO 异步模型，单机可支撑数万级长连接 |
| **WebSocket 支持** | 原生支持 WebSocket 协议，实现浏览器与服务端双向通信 |
| **Pipeline 机制** | 通过 ChannelHandler 链式处理认证、心跳、消息分发 |
| **零拷贝** | 支持 FileRegion 零拷贝，提升大消息传输性能 |

#### Netty 在本模块中的角色
```
┌─────────────────────────────────────────────────────────┐
│                    Netty Server                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ │
│  │ BossGroup   │  │ WorkerGroup │  │ ChannelPipeline │ │
│  │ (接受连接)   │  │ (处理IO)    │  │ (业务处理链)     │ │
│  └─────────────┘  └─────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### 2.2 WebSocket 的作用

#### WebSocket vs 传统方案对比
| 对比维度 | WebSocket | SSE | 短轮询 |
|---------|-----------|-----|--------|
| 通信方式 | 全双工 | 半双工 | 请求-响应 |
| 实时性 | 极高 | 高 | 取决于轮询间隔 |
| 连接开销 | 低（长连接） | 低 | 高（频繁握手） |
| 服务端推送 | 支持 | 支持 | 不支持 |
| 适用场景 | 高频双向通信 | 简单单向推送 | 低频数据查询 |

#### 为什么选择 WebSocket？
1. **多通知类型**：系统有多种通知（目标到期、饮食提醒、任务完成等），需要灵活的消息格式
2. **ACK 确认机制**：未来可能需要客户端确认收到通知，WebSocket 支持双向通信
3. **扩展性**：后续可扩展为实时聊天、协作编辑等双向交互场景

### 2.3 RabbitMQ 的作用

#### 消息流转架构
```
业务服务(diary-goal/diet/xxljob)
        │
        ▼
  RabbitMQ (notify.exchange)
        │
        ▼
  diary-notify (Consumer)
        │
        ▼
  ChannelManager 查找用户连接
        │
        ├── 用户在线 → WebSocket 推送
        └── 用户离线 → 存入离线消息表
```

#### 为什么需要 MQ？
| 好处 | 说明 |
|------|------|
| **解耦** | 业务服务不需要知道推送细节，只需发送消息到 MQ |
| **异步** | 推送失败不影响业务主流程 |
| **削峰** | 高并发场景下缓冲消息，保护推送服务 |
| **可靠** | MQ 持久化保证消息不丢失 |

---

## 三、功能点清单

### 3.1 核心功能
| 功能 | 说明 | 优先级 |
|------|------|--------|
| WebSocket 连接建立 | 客户端通过 WebSocket 连接到服务端 | P0 |
| JWT 认证 | 连接时验证用户身份 | P0 |
| 消息推送 | 将通知消息实时推送给指定用户 | P0 |
| 心跳检测 | 检测连接存活，清理失效连接 | P0 |
| Channel 管理 | 维护 userId ↔ Channel 映射关系 | P0 |

### 3.2 扩展功能
| 功能 | 说明 | 优先级 |
|------|------|--------|
| 离线消息 | 用户不在线时存储消息，上线后补推 | P1 |
| 消息确认 | 客户端收到消息后返回 ACK | P1 |
| 广播通知 | 向所有在线用户广播系统公告 | P2 |
| 消息持久化 | 将通知记录入库，支持历史查询 | P2 |

### 3.3 通知类型
| 类型 | 来源模块 | 场景 |
|------|---------|------|
| GOAL_DUE | diary-goal | 阶段目标到期提醒 |
| GOAL_PROGRESS | diary-goal | 目标进度落后提醒 |
| DIET_REMIND | diary-diet | 饭点提醒、记录提醒 |
| TASK_COMPLETE | diary-xxljob | 定时任务完成通知 |
| AI_COMPLETE | diary-AI | AI 分析完成通知 |
| FILE_READY | diary-file | 文件处理完成通知 |

---

## 四、包结构设计

```
src/main/java/diary/notify/
├── DiaryNotifyApplication.java          -- Spring Boot 启动类
│
├── config/                              -- 配置层
│   └── Netty服务器配置、线程池配置等
│
├── server/                              -- Netty 服务端核心
│   └── initializer/
│       └── ChannelInitializer Pipeline配置
│
├── handler/                             -- Netty Handler 层
│   ├── auth/                            -- JWT认证Handler
│   ├── heartbeat/                       -- 心跳检测Handler
│   ├── notify/                          -- 通知消息分发Handler
│   └── exception/                       -- 异常处理Handler
│
├── manager/                             -- 连接与消息管理
│   ├── channel/                         -- Channel生命周期管理
│   └── offline/                         -- 离线消息管理
│
├── consumer/                            -- MQ 消息消费层
│   └── RabbitMQ 通知消息消费者
│
├── protocol/                            -- 协议定义层
│   ├── message/                         -- 通知消息协议实体
│   └── codec/                           -- 消息编解码器
│
├── enums/                               -- 枚举定义
│   └── 通知类型枚举
│
└── mapper/                              -- 持久层
    └── 离线消息Mapper
```

### 包职责说明
| 包 | 职责 | 核心类示例 |
|----|------|-----------|
| config | Netty 服务器启动配置 | NettyServerConfig |
| server | WebSocket 服务端 | NotifyWebSocketServer |
| server/initializer | Pipeline 配置 | NotifyChannelInitializer |
| handler/auth | 连接认证 | AuthHandler |
| handler/heartbeat | 心跳处理 | HeartbeatHandler |
| handler/notify | 消息分发 | NotifyMessageHandler |
| manager/channel | Channel 管理 | ChannelManager |
| manager/offline | 离线消息 | OfflineMessageManager |
| consumer | MQ 消费 | NotifyMessageConsumer |
| protocol/message | 消息实体 | NotifyMessage |
| protocol/codec | 编解码 | NotifyMessageCodec |

---

## 五、架构设计

### 5.1 整体架构图
```
┌─────────────────────────────────────────────────────────────────────┐
│                           前端 (WebSocket Client)                   │
└─────────────────────────────────────────────────────────────────────┘
                                ▲
                                │ WebSocket 长连接
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        diary-notify 模块                            │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    Netty WebSocket Server                    │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │   │
│  │  │   Auth   │→ │Heartbeat │→ │  Notify  │→ │ Exception│   │   │
│  │  │ Handler  │  │ Handler  │  │ Handler  │  │ Handler  │   │   │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                │                                    │
│  ┌─────────────────┐  ┌───────┴────────┐  ┌─────────────────┐     │
│  │  ChannelManager │  │OfflineManager  │  │ MQ Consumer     │     │
│  │  (连接管理)      │  │(离线消息管理)   │  │ (消息消费)       │     │
│  └─────────────────┘  └────────────────┘  └─────────────────┘     │
└─────────────────────────────────────────────────────────────────────┘
                                ▲
                                │ 消费消息
                                │
┌─────────────────────────────────────────────────────────────────────┐
│                         RabbitMQ                                    │
│                      notify.exchange                                │
└─────────────────────────────────────────────────────────────────────┘
                                ▲
                                │ 发送消息
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
┌───────┴────────┐  ┌──────────┴─────────┐  ┌──────────┴─────────┐
│  diary-goal    │  │   diary-diet       │  │  diary-xxljob      │
│  (目标提醒)     │  │  (饮食提醒)         │  │  (任务完成通知)     │
└────────────────┘  └────────────────────┘  └────────────────────┘
```

### 5.2 消息协议设计
```json
{
  "type": "NOTIFICATION",
  "notifyType": "GOAL_DUE",
  "content": "阶段目标'减肥5kg'即将到期！",
  "timestamp": 1721136000000,
  "extra": {
    "goalId": 123
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| type | String | 消息类型：NOTIFICATION / HEARTBEAT / ACK |
| notifyType | String | 通知子类 |
| content | String | 通知内容 |
| timestamp | Long | 时间戳 |
| extra | Object | 扩展数据 |

---

## 六、开发难点与风险

### 6.1 高风险项

#### 6.1.1 连接泄漏
**问题描述**：客户端异常断开时，Channel 未及时清理，导致内存泄漏。

**解决方案**：
- 心跳检测机制（IdleStateHandler），超时自动关闭
- 连接关闭时在 `channelInactive` 中清理 ChannelManager
- 定期扫描清理僵尸连接

#### 6.1.2 消息丢失
**问题描述**：用户离线时发送的消息无法送达。

**解决方案**：
- 离线消息持久化到 MySQL
- 用户上线时查询并补推离线消息
- 消息状态标记（已推送/未推送）

#### 6.1.3 集群部署问题
**问题描述**：多实例部署时，用户连接可能分布在不同实例，消息无法路由。

**解决方案**：
- Redis Pub/Sub 跨实例广播
- 或 RabbitMQ 广播模式
- 维护用户连接实例映射

### 6.2 中风险项

#### 6.2.1 内存溢出
**问题描述**：大量长连接导致内存压力。

**解决方案**：
- 合理设置 Netty 缓冲区大小
- 限制单用户最大连接数
- 监控连接数和内存使用

#### 6.2.2 消息积压
**问题描述**：MQ 消息消费速度跟不上生产速度。

**解决方案**：
- 增加消费者实例
- 消息批量处理
- 设置消息 TTL 避免过期消息堆积

#### 6.2.3 认证失败处理
**问题描述**：JWT 过期或无效时的连接处理。

**解决方案**：
- 握手阶段验证 Token
- Token 过期主动断开连接
- 客户端收到断开事件后重新登录

### 6.3 低风险项

#### 6.3.1 协议兼容性
**问题描述**：消息协议变更时前后端不一致。

**解决方案**：
- 协议版本号管理
- 向后兼容设计
- 前端容错处理

#### 6.3.2 日志过多
**问题描述**：高频消息产生大量日志。

**解决方案**：
- 日志级别控制
- 异步日志
- 日志采样

---

## 七、最佳实践

### 7.1 Netty 配置
```java
@Bean
public ServerBootstrap serverBootstrap() {
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(notifyChannelInitializer)
        .option(ChannelOption.SO_BACKLOG, 1024)
        .childOption(ChannelOption.SO_KEEPALIVE, true);
    return bootstrap;
}
```

### 7.2 心跳配置
```java
// 读空闲60秒触发心跳检测
pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
pipeline.addLast(new HeartbeatHandler());
```

### 7.3 Channel 管理
```java
@Component
public class ChannelManager {
    private final Map<Long, Channel> userChannels = new ConcurrentHashMap<>();
    
    public void addChannel(Long userId, Channel channel) {
        userChannels.put(userId, channel);
    }
    
    public void removeChannel(Long userId) {
        userChannels.remove(userId);
    }
    
    public Channel getChannel(Long userId) {
        return userChannels.get(userId);
    }
}
```

---

## 八、模块职责边界

### 8.1 diary-notify 负责
- Netty WebSocket Server 启动与维护
- 用户连接管理与认证
- 消息推送与离线存储
- MQ 消息消费

### 8.2 diary-common 负责
- 通知消息公共实体（NotifyMessage）
- 通知类型枚举（NotifyTypeEnum）
- 跨模块复用的协议定义

### 8.3 业务模块负责
- 发送通知消息到 MQ
- 定义业务触发规则

---

## 九、维护建议

1. **监控告警**
   - 监控连接数、消息吞吐量
   - 设置连接数阈值告警
   - 监控 MQ 消费延迟

2. **日志管理**
   - 关键操作记录审计日志
   - 异常连接记录详细堆栈
   - 定期清理历史日志

3. **性能调优**
   - 根据实际负载调整 Netty 线程数
   - 优化消息序列化方式
   - 合理设置心跳超时时间

4. **安全加固**
   - JWT Token 有效期控制
   - 消息内容加密（敏感信息）
   - 防止恶意连接攻击
