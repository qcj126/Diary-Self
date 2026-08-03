# diary-goal 接口文档

## 模块说明

- 模块名称：diary-goal
- 基础路径：/goal
- 网关路径：http://gateway:10000/goal/**
- 主要职责：阶段目标、子目标的新增、批量新增、删除、修改、查询与导出。

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
| 1 | POST | /goal/add | 新增阶段目标，可携带子目标列表 |
| 2 | POST | /goal/batch/addSubGoal | 批量新增子目标 |
| 3 | POST | /goal/delete/{id} | 根据目标 ID 删除阶段目标 |
| 4 | POST | /goal/update | 修改阶段目标 |
| 5 | POST | /goal/query | 条件查询阶段目标列表 |
| 6 | GET | /goal/query/{id} | 根据 ID 查询阶段目标详情 |
| 7 | POST | /goal/export?exportType=1&lastDays=7&exportSize=10 | 导出目标数据 |

## 数据结构

### StageGoalDTO

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 阶段目标 ID，更新时必传 |
| userId | Long | 用户 ID |
| creator | String | 创建者显示名 |
| category | String | 目标分类 |
| title | String | 目标标题 |
| description | String | 目标描述 |
| subGoals | List<SubGoalDTO> | 子目标列表 |

### SubGoalDTO

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 子目标 ID |
| stageId | Long | 所属阶段目标 ID |
| userId | Long | 用户 ID |
| title | String | 子目标标题 |
| content | String | 子目标内容 |
| learnedHours | BigDecimal | 已学习/投入小时数 |
| estimatedHours | BigDecimal | 预估小时数 |

### GoalQueryDTO

| 字段 | 类型 | 说明 |
|---|---|---|
| userId | Long | 用户 ID |
| category | String | 分类筛选 |
| title | String | 标题关键字 |
| recentDays | Integer | 查询最近 N 天更新的数据 |

## 示例

```http
POST /goal/add
Content-Type: application/json

{
  "userId": 10000,
  "creator": "demo",
  "category": "学习",
  "title": "掌握算法",
  "description": "三个月完成算法训练",
  "subGoals": [
    {
      "title": "数组与链表",
      "content": "完成基础题",
      "estimatedHours": 12
    }
  ]
}
```

/goal/query 的请求体可为空；响应 data 为 List<StageGoalVO>，每个阶段目标包含 subGoals。
