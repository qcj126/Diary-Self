# diary-goal 接口文档

## 模块说明

- 模块名称：diary-goal
- 基础路径：/goal
- 网关路径：http://gateway:10000/goal/**
- 主要职责：阶段目标、子目标及目标打卡的新增、删除、修改、查询与导出。

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
| 8 | POST | /goal/checkin/add | 新增目标打卡 |
| 9 | POST | /goal/checkin/update | 修改目标打卡 |
| 10 | POST | /goal/checkin/delete/{id} | 逻辑删除目标打卡 |
| 11 | GET | /goal/checkin/query/{id} | 查询打卡详情 |
| 12 | POST | /goal/checkin/query | 分页查询打卡列表 |

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

## 目标打卡

打卡接口必须经过网关访问。用户 ID 从网关注入的 `X-Auth-User-Id` 可信请求头取得，客户端不传 `userId`。

### 新增打卡

```http
POST /goal/checkin/add
Content-Type: application/json
Authorization: Bearer <access-token>

{
  "requestId": "2c11842e-7926-45a8-b451-b39561b3ba14",
  "stageGoalId": 10001,
  "subGoalId": 20001,
  "checkinType": 1,
  "spentHours": 1.5,
  "content": "完成 Spring Cloud Gateway 路由配置学习",
  "evidenceUrls": ["https://example.com/note.png"],
  "checkinDate": "2026-09-16",
  "source": 1
}
```

`requestId` 在同一用户内唯一，用于避免按钮重复点击或网络重试导致重复累计时长。针对阶段目标本身打卡时 `subGoalId` 为空，且 `spentHours` 必须为 0；只有子目标打卡会累计学习时长。

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| requestId | String | 新增必填 | 客户端生成的幂等请求 ID，最长 64 字符；修改时不可变 |
| stageGoalId | Long | 是 | 阶段目标 ID |
| subGoalId | Long | 否 | 子目标 ID；为空表示阶段目标级打卡 |
| checkinType | Integer | 否 | `1` 正常打卡、`2` 完成目标、`3` 重新开始、`4` 补打卡，默认 `1` |
| spentHours | BigDecimal | 否 | 本次投入小时数，默认 `0`，最多两位小数 |
| content | String | 否 | 打卡内容，最长 1000 字符 |
| evidenceUrls | List<String> | 否 | 凭证 URL，最多 20 个 |
| checkinDate | LocalDate | 否 | 打卡日期，默认当天且不能晚于当天 |
| source | Integer | 否 | `1` Web、`2` App、`3` 系统、`4` 导入，默认 `1` |

新增、修改和删除打卡会在同一事务中同步 `sub_goal.learned_hours`。完成或重新开始类型会重新计算子目标及阶段目标状态。

### 修改打卡

```http
POST /goal/checkin/update
Content-Type: application/json
Authorization: Bearer <access-token>

{
  "id": 30001,
  "checkinType": 1,
  "spentHours": 2.0,
  "content": "补充完成配置与测试",
  "evidenceUrls": [],
  "checkinDate": "2026-09-16",
  "source": 1
}
```

修改时不允许改变 `requestId`、`stageGoalId` 和 `subGoalId`；如需转移到其他目标，应删除后重新打卡。

### 查询打卡列表

```http
POST /goal/checkin/query
Content-Type: application/json
Authorization: Bearer <access-token>

{
  "stageGoalId": 10001,
  "subGoalId": null,
  "checkinType": null,
  "startDate": "2026-09-01",
  "endDate": "2026-09-30",
  "pageNum": 0,
  "pageSize": 20
}
```

`pageNum` 从 0 开始，`pageSize` 最大为 100。查询结果只包含当前登录用户的数据。
