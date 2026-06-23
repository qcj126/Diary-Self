# diary-utils 模块接口文档

## 模块说明

diary-utils 是工具类模块，**不提供独立的HTTP接口**。

本模块主要为其他业务模块提供工具类支持。

## 工具类使用说明

### 1. JWT工具类 - JwtUtil

**功能**: JWT令牌生成与解析

**配置**:
```yaml
jwt:
  secret: your256bitsecretkeymustbeatleast32characterslong
  access-token-expiration: 600000    # 10分钟
  refresh-token-expiration: 21600000 # 6小时
```

**使用示例**:
```java
@Autowired
private JwtUtil jwtUtil;

// 生成Access Token
String accessToken = jwtUtil.generateAccessToken(username, roles);

// 生成Refresh Token
String refreshToken = jwtUtil.generateRefreshToken(username, roles);

// 解析Token
String username = jwtUtil.extractUsername(token);
List<String> roles = jwtUtil.extractRoles(token);
String tokenId = jwtUtil.extractTokenId(token);

// 验证Token类型
boolean isAccess = jwtUtil.isAccessToken(token);
boolean isRefresh = jwtUtil.isRefreshToken(token);
```

**方法说明**:

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| generateAccessToken | username, roles | String | 生成Access Token |
| generateRefreshToken | username, roles | String | 生成Refresh Token |
| extractUsername | token | String | 提取用户名 |
| extractRoles | token | List<String> | 提取角色列表 |
| extractTokenId | token | String | 提取Token ID |
| extractExpiration | token | Date | 提取过期时间 |
| isAccessToken | token | boolean | 是否为Access Token |
| isRefreshToken | token | boolean | 是否为Refresh Token |

---

### 2. OSS工具类 - OssUtil

**功能**: 阿里云OSS操作

**配置**:
```yaml
aliyun:
  oss:
    endpoint: oss-cn-chengdu.aliyuncs.com
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    bucket-name: your-bucket-name
    region: cn-chengdu
```

**使用示例**:
```java
@Autowired
private OssUtil ossUtil;

// 根据Object Key生成签名URL
String signedUrl = ossUtil.generateSignedUrlByKey(objectKey);

// 根据文件名生成签名URL
String signedUrl = ossUtil.getSignedUrlByFileName(fileName);

// 从URL提取Object Key
String objectKey = ossUtil.extractObjectKeyFromUrl(ossUrl);
```

**方法说明**:

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| generateSignedUrlByKey | ossUrl | String | 生成签名URL（有效期5分钟） |
| getSignedUrlByFileName | fileName | String | 根据文件名生成签名URL |
| extractObjectKeyFromUrl | ossUrl | String | 从URL提取Object Key |

---

### 3. 加密工具类 - BCryptUtilNoSpring

**功能**: BCrypt密码加密

**使用示例**:
```java
@Autowired
private BCryptUtilNoSpring bcryptUtil;

// 加密密码
String encryptedPassword = bcryptUtil.encrypt(rawPassword);

// 验证密码
boolean matches = bcryptUtil.matches(rawPassword, encryptedPassword);
```

**方法说明**:

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| encrypt | rawPassword | String | 加密密码 |
| matches | rawPassword, encryptedPassword | boolean | 验证密码 |

---

### 4. 雪花算法工具类 - SnowflakeIdUtil

**功能**: 分布式唯一ID生成

**使用示例**:
```java
// 生成唯一ID
long id = SnowflakeIdUtil.nextId();
```

**方法说明**:

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| nextId | 无 | long | 生成雪花ID |

---

### 5. 通用工具类 - MyUtils

**功能**: 通用工具方法

**使用示例**:
```java
// 生成主键ID
long id = MyUtils.getPrimaryKey();

// 判断字符串是否为空
boolean isEmpty = MyUtils.isEmpty(str);

// 判断对象是否为空
boolean isObjEmpty = MyUtils.isObjEmpty(obj);

// 判断文件是否为空
boolean isFileEmpty = MyUtils.isFileEmpty(file);

// 对象转JSON
String json = MyUtils.objectToJson(obj);

// JSON转对象
MyClass obj = MyUtils.jsonToObject(json, MyClass.class);

// 对象转字符串
String str = MyUtils.objectToString(obj);
```

**方法说明**:

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| getPrimaryKey | 无 | long | 生成主键ID（雪花算法） |
| isEmpty | str | boolean | 判断字符串是否为空 |
| isObjEmpty | obj | boolean | 判断对象是否为空 |
| isFileEmpty | file | boolean | 判断文件是否为空 |
| objectToJson | obj | String | 对象转JSON字符串 |
| jsonToObject | json, clazz | T | JSON字符串转对象 |
| objectToString | obj | String | 对象转字符串 |

---

### 6. Redis工具类 - ValidatUtil

**功能**: 验证码管理

**使用示例**:
```java
@Autowired
private ValidatUtil validatUtil;

// 存储验证码
validatUtil.saveCode(phone, code);

// 验证验证码
boolean valid = validatUtil.validateCode(phone, code);
```

**方法说明**:

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| saveCode | phone, code | void | 存储验证码 |
| validateCode | phone, code | boolean | 验证验证码 |

---

### 7. 邮箱校验工具类 - EmailValidationUtil

**功能**: 邮箱格式校验

**使用示例**:
```java
boolean isValid = EmailValidationUtil.isValidEmail(email);
```

**方法说明**:

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| isValidEmail | email | boolean | 验证邮箱格式 |

---

### 8. 手机号校验工具类 - PhoneValidationUtil

**功能**: 手机号格式校验

**使用示例**:
```java
boolean isValid = PhoneValidationUtil.isValidPhone(phone);
```

**方法说明**:

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| isValidPhone | phone | boolean | 验证手机号格式 |

---

### 9. 文件工具类 - FileUtil

**功能**: 文件操作

**方法说明**:

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| downloadFile | url, path | void | 下载文件 |
| saveFile | file, path | void | 保存文件 |

## 注意事项

1. **JWT密钥安全**
   - 生产环境必须使用强密钥
   - 定期轮换密钥
   - 不要将密钥提交到代码仓库

2. **OSS凭证安全**
   - 使用RAM角色或STS临时凭证
   - 限制Bucket访问权限
   - 签名URL设置合理的过期时间

3. **雪花算法**
   - 确保各节点Worker ID不重复
   - 处理时钟回拨问题

4. **密码加密**
   - BCrypt加密较慢，建议异步处理
   - 不要明文存储密码
