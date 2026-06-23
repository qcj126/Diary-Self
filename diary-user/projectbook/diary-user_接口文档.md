# diary-user 模块接口文档

## 模块说明

**模块名称**: diary-user  
**服务端口**: 8801  
**基础路径**: /user  
**网关路径**: http://gateway:10000/user/**

## 接口列表

### 1. 用户登录

**接口地址**: POST `/user/login`

**接口描述**: 用户登录（支持密码登录和验证码登录）

**权限要求**: 无需认证

**请求参数**:
```json
{
  "username": "string",      // 用户名（密码登录时必填）
  "password": "string",      // 密码（密码登录时必填）
  "phone": "string",         // 手机号（验证码登录时必填）
  "code": "string",          // 验证码（验证码登录时必填）
  "type": 1,                 // 登录类型：1-密码登录，2-验证码登录
  "roles": ["user"]          // 角色列表（可选）
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 条件 | 用户名，密码登录时必填 |
| password | String | 条件 | 密码，密码登录时必填 |
| phone | String | 条件 | 手机号，验证码登录时必填 |
| code | String | 条件 | 验证码，验证码登录时必填 |
| type | Integer | 是 | 登录类型：1-密码登录，2-验证码登录 |
| roles | List<String> | 否 | 角色列表 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 123456,
    "username": "testuser",
    "email": "test@example.com",
    "phone": "13800138000",
    "roles": ["user"],
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "accessTokenExpiresIn": 600000,
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshTokenExpiresIn": 21600000
  },
  "timestamp": 1234567890
}
```

**请求示例**:
```bash
curl -X POST http://localhost:8801/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "type": 1
  }'
```

---

### 2. Token刷新

**接口地址**: POST `/user/refresh`

**接口描述**: 使用Refresh Token获取新的Token对

**权限要求**: 无需认证

**请求参数**:
```json
{
  "refreshToken": "string"  // Refresh Token（必填）
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| refreshToken | String | 是 | Refresh Token |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "accessTokenExpiresIn": 600000,
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshTokenExpiresIn": 21600000
  },
  "timestamp": 1234567890
}
```

**请求示例**:
```bash
curl -X POST http://localhost:8801/user/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

---

### 3. 用户注册

**接口地址**: POST `/user/register`

**接口描述**: 新用户注册

**权限要求**: 无需认证

**请求参数**:
```json
{
  "username": "string",      // 用户名（必填）
  "password": "string",      // 密码（必填）
  "email": "string",         // 邮箱（可选）
  "phone": "string"          // 手机号（可选）
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |
| email | String | 否 | 邮箱 |
| phone | String | 否 | 手机号 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "注册成功",
  "timestamp": 1234567890
}
```

**请求示例**:
```bash
curl -X POST http://localhost:8801/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "email": "newuser@example.com"
  }'
```

---

### 4. 添加用户

**接口地址**: POST `/user/add`

**接口描述**: 管理员添加用户

**权限要求**: admin角色

**请求参数**:
```json
{
  "username": "string",      // 用户名（必填）
  "password": "string",      // 密码（必填）
  "email": "string",         // 邮箱（可选）
  "phone": "string",         // 手机号（可选）
  "roles": ["user"]          // 角色列表（可选，默认user）
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |
| email | String | 否 | 邮箱 |
| phone | String | 否 | 手机号 |
| roles | List<String> | 否 | 角色列表：admin/user |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "添加用户成功",
  "timestamp": 1234567890
}
```

**请求示例**:
```bash
curl -X POST http://localhost:10000/user/add \
  -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "roles": ["user"]
  }'
```

---

### 5. 删除用户

**接口地址**: POST `/user/delete`

**接口描述**: 管理员删除用户

**权限要求**: admin角色

**请求参数**:
```json
{
  "username": "string"  // 用户名（必填）
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "删除用户成功",
  "timestamp": 1234567890
}
```

**请求示例**:
```bash
curl -X POST http://localhost:10000/user/delete \
  -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser"
  }'
```

---

### 6. 查询用户列表

**接口地址**: GET `/user/query`

**接口描述**: 查询用户列表

**权限要求**: admin或user角色

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "userId": 123456,
      "username": "testuser",
      "email": "test@example.com",
      "phone": "13800138000",
      "status": 1,
      "roles": ["user"],
      "createTime": "2024-01-01T12:00:00"
    }
  ],
  "timestamp": 1234567890
}
```

**请求示例**:
```bash
curl -X GET http://localhost:10000/user/query \
  -H "Authorization: Bearer {token}"
```

---

### 7. 获取验证码

**接口地址**: POST `/user/verifycode`

**接口描述**: 获取验证码（用于验证码登录或密码重置）

**权限要求**: 无需认证

**请求参数**:
```json
{
  "phone": "string"  // 手机号（必填）
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | String | 是 | 手机号 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "message": "验证码已发送"
  },
  "timestamp": 1234567890
}
```

**请求示例**:
```bash
curl -X POST http://localhost:8801/user/verifycode \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "13800138000"
  }'
```

---

### 8. 重置密码

**接口地址**: POST `/user/resetPw`

**接口描述**: 忘记密码重置

**权限要求**: 无需认证

**请求参数**:
```json
{
  "username": "string",      // 用户名（必填）
  "phone": "string",         // 手机号（必填）
  "code": "string",          // 验证码（必填）
  "password": "string"       // 新密码（必填）
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名 |
| phone | String | 是 | 手机号 |
| code | String | 是 | 验证码 |
| password | String | 是 | 新密码 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "message": "密码重置成功"
  },
  "timestamp": 1234567890
}
```

**请求示例**:
```bash
curl -X POST http://localhost:8801/user/resetPw \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "phone": "13800138000",
    "code": "123456",
    "password": "newpassword123"
  }'
```

---

### 9. 查询Token信息

**接口地址**: GET `/user/token/query`

**接口描述**: 查询用户的Token信息

**权限要求**: admin角色

**请求参数**:
```
username=testuser  // Query参数
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名（Query参数） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "tokenId": "uuid-string",
      "username": "testuser",
      "roles": ["user"],
      "issueTime": "2024-01-01T12:00:00",
      "expireTime": "2024-01-01T12:10:00"
    }
  ],
  "timestamp": 1234567890
}
```

**请求示例**:
```bash
curl -X GET "http://localhost:10000/user/token/query?username=testuser" \
  -H "Authorization: Bearer {admin_token}"
```

---

### 10. 踢出用户

**接口地址**: POST `/user/token/kickout`

**接口描述**: 踢出指定用户的所有Token

**权限要求**: admin角色

**请求参数**:
```json
{
  "username": "string"  // 用户名（必填）
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "kick out success",
  "timestamp": 1234567890
}
```

**请求示例**:
```bash
curl -X POST http://localhost:10000/user/token/kickout \
  -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser"
  }'
```

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未授权（Token无效或过期） |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 注意事项

1. **认证要求**
   - 标注"无需认证"的接口可直接访问
   - 其他接口需要在Header中携带有效的Access Token
   - Token格式：`Authorization: Bearer {token}`

2. **密码要求**
   - 建议密码长度8-20位
   - 包含字母和数字
   - 建议使用特殊字符增强安全性

3. **验证码**
   - 验证码有效期5分钟
   - 每分钟最多发送1次
   - 每天最多发送10次

4. **Token有效期**
   - Access Token: 10分钟
   - Refresh Token: 6小时
   - 过期后需重新登录
