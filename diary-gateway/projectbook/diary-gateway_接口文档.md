# diary-gateway 接口文档

## 模块说明

- 模块名称：diary-gateway
- 模块类型：网关模块
- 主要职责：统一入口、路由转发、鉴权过滤、跨域和服务治理。
- 默认网关入口：http://gateway:10000

## 对外接口

当前模块自身没有业务 REST Controller，主要转发下游业务模块接口。

## 路由约定

| 网关路径 | 下游模块 | 下游基础路径 |
|---|---|---|
| /user/** | diary-user | /user |
| /goal/** | diary-goal | /goal |
| /diet/** | diary-diet | /diet |
| /file/** | diary-file | /file |
| /recipe/** | diary-recipe | /recipe |
| /time-machine/** | diary-timemachine | /time-machine |
| /ai/** | diary-AI | /ai |

## 鉴权约定

公开接口包括登录、注册、验证码、密码重置、Token 刷新等；其他业务接口建议通过网关携带：

```http
Authorization: Bearer access-token
```

具体放行列表、路由规则和过滤器行为以网关配置和安全配置为准。
