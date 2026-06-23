# diary-diet 模块接口文档

## 模块说明

**模块名称**: diary-diet  
**服务端口**: 8805  
**基础路径**: /diet  
**网关路径**: http://gateway:10000/diet/**

## 接口列表

### 1. 新增饮食记录

**接口地址**: POST `/diet/add`

**接口描述**: 新增一条饮食记录

**权限要求**: 需要认证

**请求参数**:
```json
{
  "userId": 123456,
  "eatTime": "2024-01-01T12:00:00",
  "mealType": 20,
  "foodName": "红烧肉",
  "calories": 500,
  "protein": 25.5,
  "fat": 30.0,
  "carbohydrate": 20.0,
  "fullnessScore": 8,
  "location": "家",
  "note": "很好吃"
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |
| eatTime | LocalDateTime | 是 | 食用时间 |
| mealType | Byte | 是 | 餐别：10早餐 15早加餐 20午餐 25午加餐 30晚餐 35夜宵 |
| foodName | String | 是 | 食物名称（最多200字符） |
| calories | Integer | 是 | 热量（千卡） |
| protein | BigDecimal | 是 | 蛋白质(g) |
| fat | BigDecimal | 是 | 脂肪(g) |
| carbohydrate | BigDecimal | 是 | 碳水化合物(g) |
| fullnessScore | Byte | 否 | 饱腹感评分 1~10 |
| location | String | 否 | 用餐地点（最多100字符） |
| note | String | 否 | 备注（最多500字符） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "新增成功",
  "timestamp": 1234567890
}
```

---

### 2. 删除饮食记录

**接口地址**: POST `/diet/delete/{id}`

**接口描述**: 删除指定饮食记录

**权限要求**: 需要认证

**请求参数**:
- id: 记录ID（路径参数）

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

### 3. 修改饮食记录

**接口地址**: POST `/diet/update`

**接口描述**: 修改饮食记录

**权限要求**: 需要认证

**请求参数**: 同新增接口，需携带id字段

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

### 4. 根据ID查询饮食记录

**接口地址**: GET `/diet/query/{id}`

**接口描述**: 根据ID查询饮食记录详情

**权限要求**: 需要认证

**请求参数**:
- id: 记录ID（路径参数）

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 123456,
    "userId": 1,
    "eatTime": "2024-01-01T12:00:00",
    "mealType": 20,
    "mealTypeName": "午餐",
    "foodName": "红烧肉",
    "calories": 500,
    "protein": 25.5,
    "fat": 30.0,
    "carbohydrate": 20.0,
    "fullnessScore": 8,
    "location": "家",
    "note": "很好吃",
    "createdAt": "2024-01-01T12:00:00",
    "updatedAt": "2024-01-01T12:00:00"
  },
  "timestamp": 1234567890
}
```

---

### 5. 根据用户ID查询饮食记录

**接口地址**: GET `/diet/query/user/{userId}`

**接口描述**: 查询用户的所有饮食记录

**权限要求**: 需要认证

**请求参数**:
- userId: 用户ID（路径参数）

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 123456,
      "userId": 1,
      "eatTime": "2024-01-01T12:00:00",
      "mealType": 20,
      "mealTypeName": "午餐",
      "foodName": "红烧肉",
      "calories": 500
    }
  ],
  "timestamp": 1234567890
}
```

## 请求示例

### 新增饮食记录
```bash
curl -X POST http://localhost:10000/diet/add \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123456,
    "eatTime": "2024-01-01T12:00:00",
    "mealType": 20,
    "foodName": "红烧肉",
    "calories": 500,
    "protein": 25.5,
    "fat": 30.0,
    "carbohydrate": 20.0,
    "fullnessScore": 8
  }'
```

### 查询用户饮食记录
```bash
curl -X GET http://localhost:10000/diet/query/user/123456 \
  -H "Authorization: Bearer {token}"
```
