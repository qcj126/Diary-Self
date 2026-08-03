# diary-timemachine 接口文档

## 模块说明

- 模块名称：diary-timemachine
- 基础路径：/time-machine
- 网关路径：http://gateway:10000/time-machine/**
- 主要职责：时间胶囊分类与卡片的增删改查。

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
| 1 | POST | /time-machine/category/add | 新增分类 |
| 2 | POST | /time-machine/card/add | 新增卡片 |
| 3 | POST | /time-machine/category/delete | 删除分类 |
| 4 | POST | /time-machine/card/delete | 删除卡片 |
| 5 | POST | /time-machine/category/update | 修改分类 |
| 6 | POST | /time-machine/card/update | 修改卡片 |
| 7 | POST | /time-machine/category/query | 查询分类列表 |
| 8 | POST | /time-machine/card/query?pageIndex=1&pageSize=25 | 分页查询卡片 |

## 数据结构

### TimeCategoryDTO

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 分类 ID |
| userId | Long | 用户 ID |
| categoryName | String | 分类名称 |
| deleted | Integer | 删除标记 |
| sort | Integer | 排序 |

### TimeCardDTO

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 卡片 ID |
| userId | Long | 用户 ID |
| imageId | Long | 图片 ID |
| categoryId | Long | 分类 ID |
| cardTitle | String | 卡片标题 |
| cardContent | String | 卡片内容 |
| recordTime | Date | 事件记录时间 |
| deleted | Integer | 删除标记 |

## 示例

```http
POST /time-machine/card/add
Content-Type: application/json

{
  "userId": 10000,
  "imageId": 10,
  "categoryId": 1,
  "cardTitle": "第一次旅行",
  "cardContent": "记录一段重要回忆",
  "recordTime": "2026-08-03T12:00:00.000+08:00"
}
```

分类查询响应 data 为 List<TimeCategoryVO>；卡片查询响应 data 为 MyBatis Plus IPage<TimeCardVO>。
