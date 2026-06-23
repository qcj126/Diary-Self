# diary-gateway 模块接口文档

## 模块说明

**模块名称**: diary-gateway  
**服务端口**: 10000  
**模块类型**: API网关  
**技术栈**: Spring Cloud Gateway + WebFlux

## 网关说明

diary-gateway是API网关模块，**不提供业务接口**，仅负责请求路由和过滤。

所有业务接口通过网关转发访问，网关提供统一的入口点。

## 路由规则

### 路由表

| 路由路径 | 目标服务 | 目标端口 | 说明 |
|---------|---------|---------|------|
| /user/** | diary-user | 8801 | 用户服务 |
| /file/** | diary-file | 8802 | 文件服务 |
| /recipe/** | diary-recipe | 8803 | 食谱服务 |
| /time-machine/** | diary-timemachine | 8804 | 时光机服务 |
| /diet/** | diary-diet | 8805 | 饮食服务 |

## 访问方式

### 直接访问服务
```
http://localhost:8801/user/login
http://localhost:8802/file/upload/images
```

### 通过网关访问（推荐）
```
http://localhost:10000/user/login
http://localhost:10000/file/upload/images
```

## 认证机制

### 白名单路径（无需认证）
以下路径无需Token即可访问：

| 路径 | 说明 |
|------|------|
| /user/login | 用户登录 |
| /user/register | 用户注册 |
| /user/verifycode | 获取验证码 |
| /user/resetPw | 重置密码 |
| /user/refresh | Token刷新 |
| /actuator/** | 监控端点 |
| /favicon.ico | 网站图标 |

### 需要认证的路径
除白名单外的所有路径都需要在请求Header中携带有效的Access Token。

**Token格式**:
```
Authorization: Bearer {accessToken}
```

### 角色权限

| 路径包含 | 需要角色 | 说明 |
|---------|---------|------|
| /add | admin | 添加操作需要admin角色 |
| /delete | admin | 删除操作需要admin角色 |
| 其他 | admin或user | 其他操作需要admin或user角色 |

## 请求头传递

网关验证Token后，会向下游服务传递以下请求头：

| 请求头 | 说明 | 示例 |
|--------|------|------|
| X-Auth-Username | 用户名 | testuser |
| X-Auth-Roles | 角色列表（逗号分隔） | admin,user |

下游服务可以直接从请求头获取用户信息，无需再次解析Token。

## CORS配置

网关已配置CORS跨域支持：

| 配置项 | 值 |
|--------|-----|
| 允许来源 | http://localhost:* |
| 允许方法 | GET, POST, PUT, DELETE, OPTIONS |
| 允许头部 | *（所有） |
| 允许凭证 | true |

## 错误码说明

| HTTP状态码 | 说明 |
|-----------|------|
| 200 | 成功 |
| 401 | 未授权（Token无效、过期或缺失） |
| 403 | 权限不足（角色不匹配） |
| 404 | 路由不存在 |
| 500 | 服务器内部错误 |
| 502 | 目标服务不可用 |
| 504 | 目标服务超时 |

## 使用示例

### 1. 用户登录（无需认证）

```bash
curl -X POST http://localhost:10000/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "type": 1
  }'
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

### 2. 查询用户列表（需要认证）

```bash
curl -X GET http://localhost:10000/user/query \
  -H "Authorization: Bearer {accessToken}"
```

### 3. 添加用户（需要admin角色）

```bash
curl -X POST http://localhost:10000/user/add \
  -H "Authorization: Bearer {admin_access_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123"
  }'
```

### 4. 上传图片（需要认证）

```bash
curl -X POST http://localhost:10000/file/upload/images \
  -H "Authorization: Bearer {accessToken}" \
  -F "files=@image1.jpg" \
  -F "files=@image2.jpg" \
  -F "code=1" \
  -F "userId=123456"
```

### 5. Token刷新（无需认证）

```bash
curl -X POST http://localhost:10000/user/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

## 响应格式

### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1234567890
}
```

### 错误响应

**401 未授权**:
```
HTTP/1.1 401 Unauthorized
```

**403 权限不足**:
```
HTTP/1.1 403 Forbidden
```

## 注意事项

1. **Token有效期**
   - Access Token: 10分钟
   - Refresh Token: 6小时
   - 过期后需使用Refresh Token刷新或重新登录

2. **请求头格式**
   - Authorization Header必须以"Bearer "开头
   - Bearer后面有一个空格

3. **跨域请求**
   - 浏览器会自动发送OPTIONS预检请求
   - 网关会直接放行OPTIONS请求

4. **服务可用性**
   - 确保目标服务已启动
   - 检查路由配置是否正确

5. **生产环境**
   - 应修改CORS允许的来源
   - 应使用HTTPS
   - 应部署多实例保证高可用

## 监控端点

网关暴露了Prometheus监控端点：

| 端点 | 说明 |
|------|------|
| /actuator/health | 健康检查 |
| /actuator/prometheus | Prometheus指标 |

**访问示例**:
```bash
curl http://localhost:10000/actuator/health
curl http://localhost:10000/actuator/prometheus
```
