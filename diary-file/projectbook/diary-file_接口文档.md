# diary-file 模块接口文档

## 模块说明

**模块名称**: diary-file  
**服务端口**: 8802  
**基础路径**: /file  
**网关路径**: http://gateway:10000/file/**

## 接口列表

### 1. 批量上传图片

**接口地址**: POST `/file/upload/images`

**接口描述**: 批量上传图片文件

**权限要求**: 需要认证

**请求参数**:
- files: 图片文件列表（MultipartFile）
- code: 图片类型（Integer）
- userId: 用户ID（Long）

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| files | List<MultipartFile> | 是 | 图片文件列表 |
| code | Integer | 是 | 图片类型 |
| userId | Long | 是 | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [123456, 123457, 123458],
  "timestamp": 1234567890
}
```

**请求示例**:
```bash
curl -X POST http://localhost:10000/file/upload/images \
  -H "Authorization: Bearer {token}" \
  -F "files=@image1.jpg" \
  -F "files=@image2.jpg" \
  -F "code=1" \
  -F "userId=123456"
```

---

### 2. 查询图片URL

**接口地址**: POST `/file/query/images/urls`

**接口描述**: 根据图片ID批量查询图片URL

**权限要求**: 需要认证

**请求参数**:
```json
[123456, 123457, 123458]
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| imageIds | List<Long> | 是 | 图片ID列表 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 123456,
      "url": "https://bucket.oss-cn-chengdu.aliyuncs.com/image1.jpg"
    },
    {
      "id": 123457,
      "url": "https://bucket.oss-cn-chengdu.aliyuncs.com/image2.jpg"
    }
  ],
  "timestamp": 1234567890
}
```

**请求示例**:
```bash
curl -X POST http://localhost:10000/file/query/images/urls \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '[123456, 123457, 123458]'
```

---

### 3. 批量下载图片

**接口地址**: POST `/file/download/image`

**接口描述**: 批量下载图片到本地

**权限要求**: 需要认证

**请求参数**:
```json
{
  "ossUrls": ["https://...", "https://..."]
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ossUrls | List<String> | 是 | OSS URL列表 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "downloadPath": "C:\\Users\\QCJ\\Pictures\\Saved Pictures",
    "successCount": 2,
    "failCount": 0
  },
  "timestamp": 1234567890
}
```

**请求示例**:
```bash
curl -X POST http://localhost:10000/file/download/image \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "ossUrls": [
      "https://bucket.oss-cn-chengdu.aliyuncs.com/image1.jpg",
      "https://bucket.oss-cn-chengdu.aliyuncs.com/image2.jpg"
    ]
  }'
```

---

### 4. 上传视频

**接口地址**: POST `/file/upload/video`

**接口描述**: 上传视频文件

**权限要求**: 需要认证

**请求参数**:
- file: 视频文件（MultipartFile）

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | 视频文件 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "videoId": 123456,
    "fileName": "video.mp4",
    "fileSize": 10485760
  },
  "timestamp": 1234567890
}
```

**请求示例**:
```bash
curl -X POST http://localhost:10000/file/upload/video \
  -H "Authorization: Bearer {token}" \
  -F "file=@video.mp4"
```

## 注意事项

1. **文件大小限制**
   - 单个文件最大10MB
   - 请求最大20MB

2. **文件类型**
   - 图片：jpg, jpeg, png, gif, webp
   - 视频：mp4, avi, mov

3. **异步处理**
   - 上传接口先返回ID
   - 实际上传到OSS是异步的
   - 可通过查询接口确认上传状态

4. **签名URL**
   - URL有效期5分钟
   - 过期后需重新获取
