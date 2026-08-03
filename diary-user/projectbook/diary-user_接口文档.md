# diary-user 接口文档

## 模块说明

- 模块名称：diary-user
- 基础路径：/user
- 网关路径：http://gateway:10000/user/**
- 主要职责：用户登录、注册、账号管理、验证码、密码重置、Token 查询与踢出。

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

| 序号 | 方法 | 路径 | 说明 | 权限 |
|---:|---|---|---|---|
| 1 | POST | /user/login | 用户登录 | 公开 |
| 2 | POST | /user/refresh | 使用 refreshToken 刷新 Token | 公开 |
| 3 | POST | /user/register | 用户注册 | 公开 |
| 4 | POST | /user/add | 管理员新增用户 | admin |
| 5 | POST | /user/delete | 管理员删除用户 | admin |
| 6 | GET | /user/query | 查询用户列表 | admin 或 user |
| 7 | POST | /user/verifycode | 发送/校验验证码 | 公开 |
| 8 | POST | /user/resetPw | 重置密码 | 公开 |
| 9 | GET | /user/token/query?username={username} | 查询用户 Token 信息 | admin |
| 10 | POST | /user/token/kickout | 强制用户下线 | admin |

## 数据结构

### UserReqDTO

| 字段 | 类型 | 说明 |
|---|---|---|
| username | String | 用户名 |
| password | String | 密码 |
| email | String | 邮箱 |
| phone | String | 手机号 |
| code | String | 验证码 |
| type | Integer | 登录/验证码业务类型，具体枚举以服务实现为准 |
| roles | List<String> | 角色编码列表 |

### RefreshTokenDTO

| 字段 | 类型 | 说明 |
|---|---|---|
| refreshToken | String | 刷新令牌 |

### KickOutDTO

| 字段 | 类型 | 说明 |
|---|---|---|
| username | String | 要踢出的用户名 |

## 示例

### 登录

```http
POST /user/login
Content-Type: application/json

{
  "username": "demo",
  "password": "123456",
  "type": 1
}
```

响应 data 为登录结果 Map，通常包含用户信息、角色、accessToken、refreshToken 等。

### 刷新 Token

```http
POST /user/refresh
Content-Type: application/json

{
  "refreshToken": "refresh-token"
}
```

响应 data 为 TokenPairVO：tokenType、accessToken、accessTokenExpiresIn、efreshToken、efreshTokenExpiresIn。

### 查询 Token

```http
GET /user/token/query?username=demo
Authorization: Bearer access-token
```

响应 data 为 List<TokenInfoVO>，包含用户名、角色、accessTokenId、refreshTokenId、过期时间等。
