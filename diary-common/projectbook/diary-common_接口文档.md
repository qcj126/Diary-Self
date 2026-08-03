# diary-common 接口文档

## 模块说明

- 模块名称：diary-common
- 模块类型：公共依赖模块
- 主要职责：公共 DTO/VO/PO、统一响应体、跨模块消息对象、SQL 初始化脚本。

## 对外接口

当前模块不启动 Web 服务，也没有对外暴露 REST Controller，因此没有独立 HTTP 接口。

## 被其他模块复用的接口契约

### 统一响应体 ApiResponse<T>

| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | 业务状态码，成功为 200 |
| message | String | 响应消息 |
| data | T | 业务数据 |

### 主要公共对象

| 领域 | DTO/VO |
|---|---|
| 用户 | UserReqDTO、RefreshTokenDTO、KickOutDTO、UserVO、TokenPairVO、TokenInfoVO |
| 目标 | StageGoalDTO、SubGoalDTO、GoalQueryDTO、StageGoalVO、SubGoalVO |
| 饮食 | DietRecordDTO、DietRecordVO |
| 食谱 | RecipeReqDto、RecipePageReqDto、PageRespDto、RecipeVO |
| 图片/文件 | ImageDTO、ImageVO、OssUploadSuccessMsg |
| 时间胶囊 | TimeCategoryDTO、TimeCardDTO、TimeCategoryVO、TimeCardVO |
| AI | AIInvokeDTO、ImageIdUrl |
| 定时任务 | WeatherInfo、UserPushConfig、ImageCleanUpResultDTO、ImageCleanupRecord |

公共对象字段以源码 diary-common/src/main/java/diary/common/entity 为准；业务接口文档只引用对应请求和响应对象。
