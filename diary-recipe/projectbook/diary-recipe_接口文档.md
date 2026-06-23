# diary-recipe 模块接口文档

## 模块说明

**模块名称**: diary-recipe  
**服务端口**: 8803  
**基础路径**: /recipe  
**网关路径**: http://gateway:10000/recipe/**

## 接口列表

### 1. 新增食谱

**接口地址**: POST `/recipe/add`

**接口描述**: 新增一个食谱（包含食材和步骤）

**权限要求**: 需要认证

**请求参数**:
```json
{
  "authorId": 123456,
  "title": "红烧肉",
  "coverImg": "https://...",
  "description": "经典家常菜",
  "category": 0,
  "mealType": 3,
  "difficulty": 2,
  "cookingTime": 60,
  "story": "妈妈的味道",
  "isAnniversary": 0,
  "anniversaryDate": null,
  "status": 1,
  "ingredients": [
    {
      "name": "五花肉",
      "amount": "500g"
    }
  ],
  "steps": [
    {
      "stepNum": 1,
      "description": "切块焯水"
    }
  ]
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| authorId | Long | 否 | 作者ID（从token获取） |
| title | String | 是 | 标题 |
| coverImg | String | 否 | 封面图URL |
| description | String | 否 | 简介 |
| category | Integer | 否 | 分类：0-家常 1-西餐 2-甜点 3-汤粥 4-其他 |
| mealType | Integer | 否 | 餐别：1-早餐 2-午餐 3-晚餐 4-夜宵 |
| difficulty | Integer | 否 | 难度 1-3 |
| cookingTime | Integer | 否 | 烹饪时长（分钟） |
| story | String | 否 | 情感故事 |
| isAnniversary | Integer | 否 | 是否纪念日专属：0-否 1-是 |
| anniversaryDate | LocalDate | 否 | 纪念日 |
| status | Integer | 否 | 状态：0-草稿 1-上架 2-下架 |
| ingredients | List | 否 | 食材列表 |
| steps | List | 否 | 步骤列表 |

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

### 2. 分页查询食谱

**接口地址**: POST `/recipe/query`

**接口描述**: 分页查询食谱列表

**权限要求**: 需要认证

**请求参数**:
```json
{
  "pageIndex": 1,
  "pageSize": 10,
  "category": 0,
  "mealType": 3,
  "difficulty": 2,
  "isAnniversary": 0,
  "keyword": "红烧"
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageIndex | Integer | 是 | 页码（从1开始） |
| pageSize | Integer | 是 | 每页条数 |
| category | Integer | 否 | 分类筛选 |
| mealType | Integer | 否 | 餐别筛选 |
| difficulty | Integer | 否 | 难度筛选 |
| isAnniversary | Integer | 否 | 是否纪念日专属 |
| keyword | String | 否 | 标题关键词搜索 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "pageIndex": 1,
    "pageSize": 10,
    "records": [
      {
        "recipeId": 123456,
        "title": "红烧肉",
        "coverImg": "https://...",
        "description": "经典家常菜",
        "category": 0,
        "mealType": 3,
        "difficulty": 2,
        "cookingTime": 60,
        "viewCount": 1000,
        "likeCount": 50,
        "cookCount": 30
      }
    ]
  },
  "timestamp": 1234567890
}
```

---

### 3. 修改食谱

**接口地址**: POST `/recipe/update`

**接口描述**: 修改食谱信息

**权限要求**: 需要认证

**请求参数**: 同新增接口

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

### 4. 删除食谱

**接口地址**: POST `/recipe/delete`

**接口描述**: 删除食谱

**权限要求**: 需要认证

**请求参数**:
```json
{
  "recipeId": 123456
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| recipeId | Long | 是 | 食谱ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "删除成功",
  "timestamp": 1234567890
}
```

## 请求示例

### 新增食谱
```bash
curl -X POST http://localhost:10000/recipe/add \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "红烧肉",
    "description": "经典家常菜",
    "category": 0,
    "mealType": 3,
    "difficulty": 2,
    "cookingTime": 60
  }'
```

### 分页查询
```bash
curl -X POST http://localhost:10000/recipe/query \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "pageIndex": 1,
    "pageSize": 10,
    "category": 0
  }'
```
