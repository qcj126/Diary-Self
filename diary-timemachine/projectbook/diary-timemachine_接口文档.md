# diary-timemachine 模块接口文档

## 模块说明

**模块名称**: diary-timemachine  
**服务端口**: 8804  
**基础路径**: /time-machine  
**网关路径**: http://gateway:10000/time-machine/**

## 接口列表

### 1. 新增分类

**接口地址**: POST `/time-machine/category/add`

**接口描述**: 新增时间分类

**权限要求**: 需要认证

**请求参数**:
```json
{
  "userId": 123456,
  "categoryName": "旅行",
  "sort": 1
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |
| categoryName | String | 是 | 分类名称 |
| sort | Integer | 否 | 排序 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": 1,
  "timestamp": 1234567890
}
```

---

### 2. 新增卡片

**接口地址**: POST `/time-machine/card/add`

**接口描述**: 新增时间卡片

**权限要求**: 需要认证

**请求参数**:
```json
{
  "userId": 123456,
  "imageId": 789012,
  "categoryId": 345678,
  "cardTitle": "巴黎之旅",
  "cardContent": "美丽的埃菲尔铁塔",
  "recordTime": "2024-01-01T12:00:00"
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |
| imageId | Long | 否 | 图片ID |
| categoryId | Long | 是 | 分类ID |
| cardTitle | String | 是 | 卡片标题 |
| cardContent | String | 否 | 卡片内容 |
| recordTime | Date | 是 | 记录时间 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": 1,
  "timestamp": 1234567890
}
```

---

### 3. 删除分类

**接口地址**: POST `/time-machine/category/delete`

**接口描述**: 删除时间分类（逻辑删除）

**权限要求**: 需要认证

**请求参数**:
```json
{
  "id": 123456
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "删除成功",
  "timestamp": 1234567890
}
```

---

### 4. 删除卡片

**接口地址**: POST `/time-machine/card/delete`

**接口描述**: 删除时间卡片（逻辑删除）

**权限要求**: 需要认证

**请求参数**:
```json
{
  "id": 123456
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "删除成功",
  "timestamp": 1234567890
}
```

---

### 5. 修改分类

**接口地址**: POST `/time-machine/category/update`

**接口描述**: 修改时间分类

**权限要求**: 需要认证

**请求参数**:
```json
{
  "id": 123456,
  "categoryName": "旅行记录",
  "sort": 2
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "修改成功",
  "timestamp": 1234567890
}
```

---

### 6. 修改卡片

**接口地址**: POST `/time-machine/card/update`

**接口描述**: 修改时间卡片

**权限要求**: 需要认证

**请求参数**:
```json
{
  "id": 123456,
  "cardTitle": "巴黎之旅（修改）",
  "cardContent": "美丽的埃菲尔铁塔，难忘的经历"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "修改成功",
  "timestamp": 1234567890
}
```

---

### 7. 查询分类

**接口地址**: POST `/time-machine/category/query`

**接口描述**: 查询时间分类列表

**权限要求**: 需要认证

**请求参数**:
```json
{
  "userId": 123456
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 123456,
      "userId": "123456",
      "categoryName": "旅行",
      "categoryNum": 10,
      "status": 1,
      "sort": 1,
      "createTime": "2024-01-01T12:00:00"
    }
  ],
  "timestamp": 1234567890
}
```

---

### 8. 分页查询卡片

**接口地址**: POST `/time-machine/card/query`

**接口描述**: 分页查询时间卡片

**权限要求**: 需要认证

**请求参数**:
```
pageIndex=1&pageSize=25
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageIndex | Integer | 否 | 页码（默认1） |
| pageSize | Integer | 否 | 每页条数（默认25） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 123456,
        "userId": 1,
        "imageId": 789012,
        "categoryId": 345678,
        "cardTitle": "巴黎之旅",
        "cardContent": "美丽的埃菲尔铁塔",
        "recordTime": "2024-01-01T12:00:00",
        "createTime": "2024-01-01T12:00:00",
        "updateTime": "2024-01-01T12:00:00"
      }
    ],
    "total": 100,
    "size": 25,
    "current": 1,
    "pages": 4
  },
  "timestamp": 1234567890
}
```

## 请求示例

### 新增分类
```bash
curl -X POST http://localhost:10000/time-machine/category/add \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123456,
    "categoryName": "旅行",
    "sort": 1
  }'
```

### 分页查询卡片
```bash
curl -X POST "http://localhost:10000/time-machine/card/query?pageIndex=1&pageSize=10" \
  -H "Authorization: Bearer {token}"
```
