# diary-notify 接口文档

## 模块说明

- 模块名称：diary-notify
- 模块类型：实时通知模块
- 主要职责：基于 Netty/WebSocket 维护长连接，消费 MQ 通知消息并推送给在线用户。

## REST 接口

当前模块没有对外暴露普通 REST Controller，因此没有独立 HTTP 接口。

## WebSocket 接入约定

| 项目 | 说明 |
|---|---|
| 协议 | WebSocket |
| 鉴权 | 握手阶段携带 JWT Token，服务端校验用户身份 |
| 消息方向 | 服务端主动推送通知，客户端可扩展 ACK/心跳 |
| 消息来源 | RabbitMQ 通知消息，由业务模块异步投递 |

实际 WebSocket 地址、端口和 Token 传递方式以 Netty 服务配置为准。

## 通知消息建议格式

```json
{
  "type": "NOTIFICATION",
  "notifyType": "GOAL_DUE",
  "content": "阶段目标即将到期",
  "timestamp": 1785748800000,
  "extra": {
    "goalId": 123
  }
}
```

## 通知类型

| 类型 | 来源模块 | 场景 |
|---|---|---|
| GOAL_DUE | diary-goal | 阶段目标到期提醒 |
| GOAL_PROGRESS | diary-goal | 目标进度提醒 |
| DIET_REMIND | diary-diet | 饮食记录或用餐提醒 |
| TASK_COMPLETE | diary-xxljob | 定时任务执行完成 |
| AI_COMPLETE | diary-AI | AI 分析完成 |
| FILE_READY | diary-file | 文件处理完成 |
