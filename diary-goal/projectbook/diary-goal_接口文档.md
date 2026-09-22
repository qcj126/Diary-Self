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
| 7 | POST | /goal/export?exportType=1&ddl=1&category=1&lastDays=7&exportSize=10 | 导出目标数据并下载文件 |
| 8 | POST | /goal/checkin/add | 新增目标打卡 |
| 9 | POST | /goal/checkin/update | 修改目标打卡 |
| 10 | POST | /goal/checkin/delete/{id} | 逻辑删除目标打卡 |
| 11 | GET | /goal/checkin/query/{id} | 查询打卡详情 |
| 12 | POST | /goal/checkin/query | 分页查询打卡列表 |
| 13 | POST | /goal/dashboard/review | 查询周报或月报仪表盘 |

### 导出参数

`POST /goal/export` 通过网关认证后的 `X-Auth-User-Id` 导出当前用户的数据，并以附件响应返回文件。

| 参数 | 默认值 | 说明 |
|---|---:|---|
| exportType | 1 | `1` Excel，`2` PDF，`3` PNG 图片 |
| ddl | 1 | `1` 全部，`2` 正常，`3` 即将到期，`4` 已到期 |
| category | 1 | `1` 全部，`2` 技术，`3` 学习，`4` 健康，`5` 生活 |
| lastDays | 7 | 最近更新天数；传 `0` 表示不限制 |
| exportSize | 10 | 导出条数，范围 `1` 到 `100`；一条直接返回文件，多条返回 ZIP 包 |

## 数据结构

### StageGoalDTO

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 阶段目标 ID，更新时必传 |
| flag | Integer | 修改目标类型：`1` 阶段目标、`2` 小目标 |
| userId | Long | 用户 ID |
| creator | String | 新增无需传；查询时根据 `userId` 关联 `user.username` 返回 |
| category | String | 目标分类 |
| title | String | 目标标题 |
| description | String | 目标描述 |
| status | Integer | 目标状态；完成操作传 `3` |
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
| creator | String | 创建人关键字 |
| category | String | 分类筛选 |
| title | String | 标题关键字 |
| recentDays | Integer | 查询最近 N 天更新的数据 |

## 示例

```http
POST /goal/add
Content-Type: application/json

{
  "userId": 10000,
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

### 完成阶段目标或小目标

完成没有子目标的阶段目标：

```http
POST /goal/update
Content-Type: application/json

{
  "id": 10001,
  "flag": 1,
  "status": 3
}
```

完成小目标：

```http
POST /goal/update
Content-Type: application/json

{
  "id": 20001,
  "flag": 2,
  "status": 3
}
```

当同一阶段目标下的全部小目标状态均为 `3` 时，后端会在同一事务中自动将阶段目标状态更新为 `3`。包含未完成小目标的阶段目标不能直接完成。

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

## 目标仪表盘与周期复盘

仪表盘接口必须经过网关访问。用户 ID 从网关注入的 `X-Auth-User-Id` 请求头取得，客户端不能通过请求体指定用户。

```http
POST /goal/dashboard/review
Content-Type: application/json
Authorization: Bearer <access-token>

{
  "periodType": 1,
  "anchorDate": "2026-09-20",
  "staleDays": 7
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| periodType | Integer | 否 | `1` 周报、`2` 月报，默认 `1` |
| anchorDate | LocalDate | 否 | 周期锚点，默认当天且不能晚于当天；周报从周一开始，月报按自然月 |
| staleDays | Integer | 否 | 长期未更新阈值，范围 `1-3650`，默认 `7` |

请求体可以为空。当前周期的 `dataThroughDate` 为当天，历史周期为周期结束日，不会将未来日期纳入统计。

统计口径：

- 完成率分母为周期开始至 `dataThroughDate` 已到期的阶段目标，分子为其中截至统计日已完成的目标。
- 逾期包括超过自身 DDL 后完成，以及截至统计日仍未完成的目标。
- 周期投入、分类占比和连续打卡均以未删除的 `goal_checkin` 流水为准；零时长的状态打卡同样计为打卡日。
- 预计/实际偏差以周期内完成的阶段目标为范围，预计值汇总其子目标预计时长，实际值汇总完成前的有效打卡时长。
- 燃尽图以周期内有效目标的子目标预计时长减去周期开始前投入作为初始剩余量，按日扣减打卡投入。
- 长期未更新仅返回未开始、进行中、暂停状态的目标，按未活动天数倒序，最多 100 条。

响应 `data` 包含：`summary`、`timeTrend`、`categoryDistribution`、`burndown`、`staleGoals`。其中周报的 `timeTrend` 按日返回，月报按自然周返回。
