# diary-common 模块接口文档

## 模块说明

diary-common 是公共基础模块，**不提供独立的HTTP接口**。

本模块主要为其他业务模块提供：
- 公共DTO/VO实体类定义
- 统一返回结果格式
- 全局异常处理
- 常量定义
- 数据转换工具

## 接口使用说明

各业务模块（diary-user、diary-file、diary-diet、diary-recipe、diary-timemachine）的接口文档请查看对应模块的接口文档。

## 公共数据结构

### 统一返回格式 - ApiResponse

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1234567890
}
```

**字段说明：**
| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 响应码，200表示成功 |
| message | String | 响应消息 |
| data | Object | 响应数据 |
| timestamp | Long | 时间戳 |

## 常用DTO/VO

### 用户模块
- **UserReqDTO**: 用户请求参数
- **UserVO**: 用户响应数据
- **TokenPairVO**: 令牌对
- **TokenInfoVO**: 令牌信息

### 饮食模块
- **DietRecordDTO**: 饮食记录请求
- **DietRecordVO**: 饮食记录响应

### 文件模块
- **ImageDTO**: 图片请求参数
- **ImageVO**: 图片响应数据

### 食谱模块
- **RecipeReqDto**: 食谱请求
- **RecipeRespDto**: 食谱响应
- **RecipePageReqDto**: 食谱分页请求
- **PageRespDto**: 分页响应

### 时光机模块
- **TimeCardDTO**: 时间卡片请求
- **TimeCardVO**: 时间卡片响应
- **TimeCategoryDTO**: 时间分类请求
- **TimeCategoryVO**: 时间分类响应

## 常量定义

### RedisKeyConst
- `TOKEN_WHITE_PREFIX`: Token白名单前缀
- `TOKEN_BLACK_PREFIX`: Token黑名单前缀

### PhotoTypeConst
- 图片类型定义

### TimeLineCategoryConsts
- 时间线分类定义

## 异常处理

全局异常处理器会自动捕获并转换以下异常：
- **ParamIllegalException**: 参数非法异常
- **NullResultException**: 空结果异常
- **CustomException**: 自定义业务异常

异常返回格式：
```json
{
  "code": 400,
  "message": "错误信息",
  "data": null,
  "timestamp": 1234567890
}
```
