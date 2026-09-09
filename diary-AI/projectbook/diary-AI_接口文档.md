# diary-AI 接口文档

## 1. 模块说明

- 模块：`diary-AI`
- 基础路径：`/ai`
- 网关路由：`/ai/** -> lb://diary-ai`
- 职责：受理 AI 异步任务、查询任务状态与结果。

## 2. 认证约定

所有业务接口应通过 `diary-gateway` 访问。网关校验 JWT 后注入：

```http
X-Auth-User-Id: <JWT user_id>
```

`diary-AI` 不信任客户端请求体中的 `userId`。查询任务时始终使用 `taskId + userId` 校验数据归属。

## 3. 接口列表

| 方法 | 路径 | 说明 | 正常 HTTP 状态 |
| --- | --- | --- | --- |
| POST | `/ai/tasks` | 提交 AI 异步任务 | 202 |
| GET | `/ai/tasks/{taskId}` | 查询任务状态 | 200 |
| GET | `/ai/tasks/{taskId}/result` | 查询任务结果 | 处理中 202，终态 200 |

## 4. 提交 AI 任务

```http
POST /ai/tasks
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "clientRequestId": "ai-20260909-001",
  "aiType": 1,
  "materials": {
    "chicken": "150g",
    "rice": "200g"
  },
  "aiApplication": 1,
  "cookWay": "steam",
  "flag": "DIET",
  "universalId": 10001
}
```

### 4.1 请求字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `clientRequestId` | String | 是 | 客户端幂等键，同一用户内唯一 |
| `aiType` | Integer | 是 | 大模型工厂 code；当前 `1=Qwen 3.7 Plus` |
| `materials` | Map<String,String> | 是 | 食材名称与数量 |
| `aiApplication` | Integer | 是 | AI 应用类型；`1=营养分析` |
| `cookWay` | String | 否 | 烹饪方式 |
| `flag` | String | 是 | `DIET/RECIPE/GOAL` |
| `universalId` | Long | 是 | `flag` 所对应的业务数据 ID |

### 4.2 响应

```http
HTTP/1.1 202 Accepted
Location: /ai/tasks/123456789
```

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 123456789,
    "status": "PENDING",
    "message": "AI分析任务已受理"
  }
}
```

同一 `clientRequestId` 和相同内容重复提交时，返回原 `taskId`。同一幂等键对应不同内容时返回幂等冲突。

## 5. 查询任务状态

```http
GET /ai/tasks/{taskId}
Authorization: Bearer <access-token>
```

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 123456789,
    "status": "RUNNING",
    "errorCode": null,
    "errorMessage": null
  }
}
```

状态直接从 MySQL 查询，不使用 Redis 任务缓存。

## 6. 查询任务结果

```http
GET /ai/tasks/{taskId}/result
Authorization: Bearer <access-token>
```

- `PENDING/QUEUED/RUNNING/RETRY_WAIT`：HTTP 202，返回当前状态。
- `SUCCESS`：HTTP 200，返回营养结果。
- `FAILED/DEAD_LETTER`：HTTP 200，返回 `errorCode/errorMessage`。

## 7. 任务状态

| 状态 | 说明 |
| --- | --- |
| `PENDING` | 任务已创建，等待 Outbox 确认 Broker 接收 |
| `QUEUED` | Broker 已接收任务消息 |
| `RUNNING` | 模型正在执行 |
| `RETRY_WAIT` | 可重试失败，等待重投 |
| `SUCCESS` | 执行成功 |
| `FAILED` | 永久错误或重试耗尽 |
| `DEAD_LETTER` | 任务消息未能投递到 Broker |

## 8. 联调注意

- 不要直接对外暴露 `diary-AI:8807`，否则可绕过网关身份注入。
- Snowflake ID 对 JavaScript 客户端建议以字符串传输，避免精度丢失。
- 第二个模型接入时，`InvokeAIService.getCode()` 必须唯一；重复 code 会导致应用启动失败。
- 更完整的流程和验收用例见根目录 `AI应用RocketMq-版本2-实操手册.md` 和 `diary-AI简化版测试用例.md`。
