# diary-recipe 模块接口文档

## 模块说明

**模块名称**: diary-recipe  
**服务端口**: 8803  
**基础路径**: /recipe  
**网关路径**: http://gateway:10000/recipe/**

## 接口列表

### 1. 新增食谱

**接口地址**: POST `/recipe/add`

**接口描述**: 新增一个食谱，并同步新增该食谱的食材列表和步骤列表。服务层会在同一事务内写入 `recipe`、`recipe_ingredient`、`recipe_step` 三张表。

**权限要求**: 需要认证

**请求体**: `application/json`

**当前实现说明**:
- 当前 `RecipeAddServiceImpl#validateRecipe` 会将 `authorId` 临时覆盖为 `10000L`，因此请求中的 `authorId` 暂不会生效。
- 当前代码要求 `imageId` 必填；但 `DtoConvertToPo` 尚未把 `imageId` 写入 `RecipePO.coverImg`，封面落库逻辑需要后续补齐。
- 同一作者、同一餐别下不允许存在同名食谱，重复时返回 `作者在该餐别下已存在同名食谱`。
- 食材和步骤列表都不能为空；任一食材/步骤缺少必填字段会整体失败并回滚。

**请求示例**:
```json
{
  "title": "红烧肉",
  "imageId": "1860000000000000001",
  "description": "经典家常菜",
  "category": 0,
  "mealType": 3,
  "difficulty": 2,
  "cookingTime": 60,
  "story": "妈妈的味道",
  "ingredients": [
    {
      "name": "五花肉",
      "quantity": "500g",
      "isMain": 1,
      "sort": 1
    },
    {
      "name": "冰糖",
      "quantity": "20g",
      "isMain": 0,
      "sort": 2
    }
  ],
  "steps": [
    {
      "stepNumber": 1,
      "description": "五花肉切块后冷水下锅焯水。",
      "timerMin": 5,
      "sort": 1
    },
    {
      "stepNumber": 2,
      "description": "炒糖色后加入五花肉翻炒。",
      "timerMin": 8,
      "sort": 2
    }
  ]
}
```

**请求参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| authorId | Long | 否 | 作者ID。当前实现会被服务层临时覆盖为 `10000L`。 |
| title | String | 是 | 食谱标题，不能为空字符串。 |
| imageId | String | 是 | 封面图片ID/图片标识，当前仅做必填校验。 |
| description | String | 是 | 食谱简介，不能为空字符串。 |
| category | Integer | 是 | 分类：0-家常 1-西餐 2-甜点 3-汤粥 4-其他。 |
| mealType | Integer | 是 | 餐别：1-早餐 2-午餐 3-晚餐 4-夜宵。 |
| difficulty | Integer | 是 | 难度。当前 DTO 注释为 1-3。 |
| cookingTime | Integer | 是 | 烹饪时长，单位分钟。 |
| story | String | 否 | 情感故事/备注；不传时落库为空字符串。 |
| ingredients | Array | 是 | 食材列表，不能为空。 |
| steps | Array | 是 | 步骤列表，不能为空。 |

**ingredients 字段说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 食材名称，不能为空字符串。 |
| quantity | String | 是 | 食材数量，例如 `500g`、`2勺`。 |
| isMain | Integer | 是 | 是否主料：0-否 1-是。 |
| sort | Integer | 否 | 食材排序；不传时按当前转换逻辑默认 0。 |

**steps 字段说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| stepNumber | Integer | 是 | 步骤编号。 |
| description | String | 是 | 步骤描述，不能为空字符串。 |
| timerMin | Integer | 否 | 步骤计时，单位分钟；不传时按当前转换逻辑默认 0。 |
| sort | Integer | 否 | 步骤排序；不传时默认使用 `stepNumber`。 |

**成功响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "食谱添加成功"
}
```

**失败响应示例**:
```json
{
  "code": 500,
  "message": "食谱存在必填参数为空",
  "data": null
}
```

**常见失败信息**:

| 场景 | message |
|------|---------|
| 请求体为空 | 入参为空 |
| 食谱主字段、食材列表或步骤列表缺失 | 食谱存在必填参数为空 |
| 食材名称、数量或主料标识缺失 | 食材存在必填参数为空 |
| 步骤描述或步骤编号缺失 | 步骤存在必填参数为空 |
| 同一作者同一餐别下标题重复 | 作者在该餐别下已存在同名食谱 |

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
    "imageId": "1860000000000000001",
    "description": "经典家常菜",
    "category": 0,
    "mealType": 3,
    "difficulty": 2,
    "cookingTime": 60,
    "story": "妈妈的味道",
    "ingredients": [
      {
        "name": "五花肉",
        "quantity": "500g",
        "isMain": 1,
        "sort": 1
      }
    ],
    "steps": [
      {
        "stepNumber": 1,
        "description": "五花肉切块后冷水下锅焯水。",
        "timerMin": 5,
        "sort": 1
      }
    ]
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
