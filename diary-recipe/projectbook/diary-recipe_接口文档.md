# diary-recipe 接口文档

## 模块说明

- 模块名称：diary-recipe
- 基础路径：/recipe
- 网关路径：http://gateway:10000/recipe/**
- 主要职责：食谱新增、分页查询、修改和删除。

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

除登录、注册、验证码、重置密码、Token 刷新等公开接口外，业务接口通常应通过网关携带认证信息访问。

## 接口列表

| 序号 | 方法 | 路径 | 说明 |
|---:|---|---|---|
| 1 | POST | /recipe/add | 新增食谱 |
| 2 | POST | /recipe/query | 分页查询食谱 |
| 3 | POST | /recipe/update | 修改食谱 |
| 4 | POST | /recipe/delete | 删除食谱 |

## 数据结构

### RecipeReqDto

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 食谱 ID，更新/删除时必传 |
| authorId | Long | 作者用户 ID |
| title | String | 标题 |
| imageId | Long | 封面图片 ID |
| description | String | 简介 |
| category | Integer | 分类 |
| mealType | Integer | 餐别 |
| difficulty | Integer | 难度 |
| cookingTime | Integer | 烹饪时长，单位分钟 |
| story | String | 故事或备注 |
| ingredients | List<RecipeIngredientAO> | 食材列表 |
| steps | List<RecipeStepAO> | 步骤列表 |

### RecipeIngredientAO

| 字段 | 类型 | 说明 |
|---|---|---|
| name | String | 食材名称 |
| quantity | String | 用量 |
| isMain | Integer | 是否主料 |
| sort | Integer | 排序 |

### RecipeStepAO

| 字段 | 类型 | 说明 |
|---|---|---|
| stepNumber | Integer | 步骤编号 |
| description | String | 步骤描述 |
| timerMin | Integer | 计时分钟数 |
| sort | Integer | 排序 |

### RecipePageReqDto

继承分页字段：pageNum、pageSize、orderBy，并支持 category、mealType、difficulty、keyword 筛选。

## 示例

```http
POST /recipe/query
Content-Type: application/json

{
  "pageNum": 1,
  "pageSize": 20,
  "category": 0,
  "mealType": 2,
  "keyword": "鸡胸肉"
}
```

查询响应 data 为 PageRespDto<RecipeVO>，包含 	otal、pages、current、size、ecords。
