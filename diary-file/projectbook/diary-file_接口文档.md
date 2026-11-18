# diary-file 接口文档

## 模块说明

- 模块名称：`diary-file`
- 基础路径：`/file`
- 网关路径：`http://gateway:10000/file/**`
- 主要职责：图片分片上传、原图与缩略图读取、上传状态查询和图片删除。
- 详细上传字段、状态和前端示例见根目录《前端调用后端参数文档.md》。

## 通用约定

除直接返回图片流的接口外，业务接口统一返回 `ApiResponse<T>`。

## 图片接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/file/upload/images/config` | 查询分片大小、文件上限和允许的 MIME |
| POST | `/file/upload/images/init` | 初始化上传或命中 READY 秒传 |
| POST | `/file/upload/images/{uploadId}/chunks/{chunkIndex}` | 上传一个 256 KiB 分片 |
| POST | `/file/upload/images/{uploadId}/complete` | 合并并校验完整文件 |
| POST | `/file/query/images/statuses` | 批量查询图片处理状态 |
| POST | `/file/query/images/urls` | 批量查询 READY 原图地址 |
| POST | `/file/query/thumbnails/urls` | 批量查询 READY 缩略图地址 |
| GET | `/file/originals/{imageId}` | 流式读取当前用户的原图 |
| GET | `/file/thumbnails/{imageId}` | 流式读取当前用户的缩略图 |
| POST | `/file/query/images/carousel` | 查询轮播图片地址 |
| POST | `/file/delete/{id}` | 删除当前用户的图片 |

旧的整文件上传、视频上传、按文件名读取和下载到服务节点接口已经删除。原图和缩略图直接从 OSS 流式返回，不写入节点本地目录。

## 查询地址示例

请求：

```json
[1, 2, 3]
```

原图地址响应中的 `url` 形如 `/file/originals/1`，缩略图地址形如 `/file/thumbnails/1`。只有属于当前用户且状态为 `READY` 的图片才会返回。
