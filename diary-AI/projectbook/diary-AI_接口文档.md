# diary-AI 接口文档

## 模块说明

- 模块名称：`diary-AI`
- 基础路径：`/ai`
- 网关路径：`http://gateway:10000/ai/**`
- 主要职责：调用大模型处理图片或业务数据，并将分析结果写入后续业务流程。

## 通用约定

所有业务接口默认返回 `ApiResponse<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

失败时返回：

```json
{
  "code": 500,
  "message": "错误信息",
  "data": null
}
```

## 接口列表

| 序号 | 方法 | 路径 | 说明 |
|---:|---|---|---|
| 1 | POST | `/ai/invoke` | 调用 AI 处理请求 |

## AIInvokeDTO

| 字段 | 类型 | 说明 |
|---|---|---|
| aiType | Integer | AI 服务类型：1-deepseek、2-通义千问、3-豆包、4-元宝 |
| imageIdUrls | Map<Long, String> | 图片 ID 到 OSS URL 的映射，用于下载图片并分析 |
| aiApplication | Integer | AI 应用场景，具体枚举以服务实现为准 |

## 示例

```http
POST /ai/invoke
Content-Type: application/json

{
  "aiType": 1,
  "aiApplication": 1,
  "imageIdUrls": {
    "10": "https://example.com/image.jpg"
  }
}
```

成功响应：

```json
{
  "code": 200,
  "message": "success",
  "data": "调用AI成功，数据已处理"
}
```
