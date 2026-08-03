# diary-file 接口文档

## 模块说明

- 模块名称：`diary-file`
- 基础路径：`/file`
- 网关路径：`http://gateway:10000/file/**`
- 主要职责：图片上传、图片签名 URL 查询、轮播图查询、图片下载、视频上传和文件删除。

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

| 序号 | 方法 | 路径 | Content-Type | 说明 |
|---:|---|---|---|---|
| 1 | POST | `/file/upload/images` | `multipart/form-data` | 批量上传图片 |
| 2 | POST | `/file/query/images/urls` | `application/json` | 根据图片 ID 列表查询签名 URL |
| 3 | POST | `/file/query/images/carousel` | `application/json` | 查询轮播图图片 |
| 4 | POST | `/file/download/image` | `application/json` | 批量下载图片 |
| 5 | POST | `/file/upload/video` | `multipart/form-data` | 上传视频 |
| 6 | POST | `/file/delete/{id}` | `application/json` | 删除图片文件记录 |

## 参数说明

### 上传图片

| 参数 | 类型 | 位置 | 说明 |
|---|---|---|---|
| files | List<MultipartFile> | form-data | 图片文件列表 |
| code | Integer | form-data | 图片类型编码 |

响应 `data` 为 `List<Long>`，表示已入库图片 ID。

### 查询图片 URL

请求体为图片 ID 数组：

```json
[1, 2, 3]
```

响应 `data` 为 `List<ImageVO>`，字段为 `id`、`url`。

### 下载图片

请求体为图片 ID 到 URL 的映射：

```json
{
  "1": "https://example.com/a.jpg",
  "2": "https://example.com/b.jpg"
}
```

响应 `data` 为下载结果 Map。

### 上传视频

| 参数 | 类型 | 位置 | 说明 |
|---|---|---|---|
| file | MultipartFile | form-data | 视频文件 |

视频接口会先入库，再异步上传 OSS 并发送 MQ 消息。
