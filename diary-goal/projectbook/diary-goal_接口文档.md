# diary-goal 模块接口文档

## 模块说明

**模块名称**: diary-goal  
**服务端口**: 通过网关访问（网关端口: 10000）  
**基础路径**: /goal  
**网关路径**: http://gateway:10000/goal/**

## 接口列表

### 1. 新增阶段目标

**接口地址**: POST `/goal/add`

**接口描述**: 新增一个阶段目标（包含子目标列表）

**权限要求**: 需要认证

**请求参数**:
```json
{
  "userId": 123,
  "creator": "张三",
  "category": "学习",
  "title": "掌握算法",
  "description": "三个月内完成算法训练营",
  "subGoals": [
    { "title": "第1周：入门", "content": "刷题 5 道" }
  ]
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 否 | 用户ID（可从token获取） |
| creator | String | 否 | 创建者显示名 |
| category | String | 否 | 目标分类 |
| title | String | 是 | 阶段目标标题 |
| description | String | 否 | 目标描述 |
| subGoals | List<SubGoalDTO> | 否 | 子目标列表（每项包含 title, content, estimatedHours 等） |

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

### 2. 删除阶段目标

**接口地址**: POST `/goal/delete/{id}`

**接口描述**: 根据 ID 删除阶段目标

**权限要求**: 需要认证

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 阶段目标ID |

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

### 3. 更新阶段目标

**接口地址**: POST `/goal/update`

**接口描述**: 修改阶段目标信息（带 id 表示更新已有记录）

**权限要求**: 需要认证

**请求参数**: 同新增接口，但需要包含 `id` 表示要更新的目标

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

### 4. 查询阶段目标列表

**接口地址**: POST `/goal/query`

**接口描述**: 条件查询阶段目标列表（可为空，返回全部/最近项）

**权限要求**: 需要认证

**请求参数**:
```json
{
  "userId": 123,
  "creator": "张三",
  "category": "学习",
  "title": "算法",
  "recentDays": 30
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 否 | 用户ID |
| creator | String | 否 | 创建者 |
| category | String | 否 | 分类 |
| title | String | 否 | 标题关键词 |
| recentDays | Integer | 否 | 最近更新天数（过滤） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "userId": 123,
      "creator": "张三",
      "category": "学习",
      "title": "掌握算法",
      "description": "三个月内完成算法训练营",
      "learnedHours": 10.5,
      "estimatedHours": 120.0,
      "remainingHours": 109.5,
      "progress": 8,
      "createTime": "2026-07-01T12:00:00",
      "updateTime": "2026-07-10T12:00:00",
      "daysSinceUpdate": 11,
      "subGoals": [
        { "id": 11, "stageGoalId": 1, "title": "第1周：入门", "content": "刷题 5 道", "learnedHours": 1.0 }
      ]
    }
  ],
  "timestamp": 1234567890
}
```

---

### 5. 根据 ID 查询阶段目标

**接口地址**: GET `/goal/query/{id}`

**接口描述**: 获取单个阶段目标详情（含子目标）

**权限要求**: 需要认证

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 阶段目标ID |

**响应示例**: 同 查询阶段目标列表 中单个记录的 data 项

---

### 6. 导出阶段目标

**接口地址**: POST `/goal/export`

**接口描述**: 导出目标数据（支持导出类型、时间范围、导出条数）

**权限要求**: 需要认证

**请求参数（Query）**:
| 参数名 | 类型 | 默认 | 说明 |
|--------|------|------|------|
| exportType | Integer | 1 | 导出类型（1=PDF,2=图片,3=Excel 等） |
| lastDays | Integer | 7 | 最近多少天的数据 |
| exportSize | Integer | 10 | 最大导出条数 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "Export successful",
  "timestamp": 1234567890
}
```

## 常用请求示例

### 新增
```bash
curl -X POST http://localhost:10000/goal/add \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{ "title": "掌握算法", "description": "三个月" }'
```

### 查询
```bash
curl -X POST http://localhost:10000/goal/query \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{ "userId": 123 }'
```
