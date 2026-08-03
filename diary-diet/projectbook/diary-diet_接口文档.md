# diary-diet 接口文档

## 模块说明

- 模块名称：diary-diet
- 基础路径：/diet
- 网关路径：http://gateway:10000/diet/**
- 主要职责：饮食记录新增、删除、修改和查询。

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
| 1 | POST | /diet/add | 新增饮食记录 |
| 2 | POST | /diet/delete/{id} | 删除饮食记录 |
| 3 | POST | /diet/update | 修改饮食记录 |
| 4 | GET | /diet/query/{id} | 根据 ID 查询饮食记录 |
| 5 | GET | /diet/query/user/{userId} | 查询用户全部饮食记录 |

## DietRecordDTO

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 记录 ID，更新时必传 |
| userId | Long | 用户 ID |
| eatTime | LocalDateTime | 进食时间 |
| mealType | Byte | 餐别编码 |
| foodName | String | 食物名称 |
| calories | Integer | 热量，单位千卡 |
| protein | BigDecimal | 蛋白质，单位 g |
| fat | BigDecimal | 脂肪，单位 g |
| carbohydrate | BigDecimal | 碳水，单位 g |
| fullnessScore | Byte | 饱腹感评分 |
| location | String | 用餐地点 |
| note | String | 备注 |

## 示例

```http
POST /diet/add
Content-Type: application/json

{
  "userId": 10000,
  "eatTime": "2026-08-03T12:00:00",
  "mealType": 20,
  "foodName": "鸡胸肉沙拉",
  "calories": 420,
  "protein": 32.5,
  "fat": 12.0,
  "carbohydrate": 38.0,
  "fullnessScore": 8,
  "location": "家",
  "note": "午餐"
}
```

查询接口响应 data 为 DietRecordVO 或 List<DietRecordVO>，会额外返回 mealTypeName、创建时间和更新时间。
