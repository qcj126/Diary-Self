# diary-common 模块技术与提升手册

## 一、模块概述

diary-common 是Diary-Self项目的公共模块，提供项目中各业务模块共享的基础组件，包括：
- 常量定义（Consts）
- 实体类（Entity/DTO/VO）
- 数据转换器（Convert）
- 统一异常处理（Exception）
- 统一返回结果（Result）

## 二、技术栈

### 2.1 核心技术
- **Java 21**: 项目基础语言版本
- **Spring Boot 3.5.14**: 基础框架
- **Spring Boot Starter Web**: Web模块基础支持
- **Spring Boot Starter Validation**: 参数校验支持
- **Lombok 1.18.44**: 简化代码开发
- **MyBatis-Plus Core 3.5.5**: 数据库操作核心库

### 2.2 依赖管理
```xml
- spring-boot-starter-web
- spring-boot-starter-validation
- lombok
- mybatis-plus-core
```

## 三、核心功能

### 3.1 常量定义（consts）
- **FileTypeConst**: 文件类型常量
- **PhotoStatusConst**: 照片状态常量
- **PhotoTypeConst**: 照片类型常量
- **RedisKeyConst**: Redis键前缀常量
- **TimeLineCategoryConsts**: 时间线分类常量

### 3.2 实体类（entity）
按业务模块划分：
- **diet**: 饮食记录相关DTO/VO
- **file**: 文件相关实体
- **image**: 图片相关DTO/VO
- **recipe**: 食谱相关DTO/VO/AO/PO
- **timemachine**: 时光机相关DTO/VO
- **user**: 用户相关DTO/VO

### 3.3 数据转换器（convert）
- **diet**: 饮食数据转换
- **recipe**: 食谱数据转换
- **timemachine**: 时光机数据转换

### 3.4 异常处理（exception）
- **CustomException**: 自定义异常基类
- **GlobalExceptionHandler**: 全局异常处理器
- **NullResultException**: 空结果异常
- **ParamIllegalException**: 参数非法异常

### 3.5 统一返回结果（result）
- **ApiResponse**: 统一API响应格式

### 3.6 枚举类（enums）
- **dietenum**: 饮食相关枚举
- **typeenum**: 类型相关枚举

## 四、优化建议

### 4.1 代码优化
1. **DTO/VO分离优化**
   - 建议增加MapStruct进行DTO/VO/PO之间的自动转换
   - 减少手动转换代码，提高可维护性

2. **常量管理优化**
   - 建议使用枚举替代部分常量类
   - 增加常量的注释和说明文档

3. **异常处理优化**
   - 建议增加更细粒度的异常分类
   - 增加异常码管理，便于前端国际化

### 4.2 架构优化
1. **依赖优化**
   - 当前依赖了`spring-boot-starter-web`，但作为公共模块可能不需要完整的Web支持
   - 建议仅依赖`spring-boot-starter-validation`和`mybatis-plus-core`

2. **包结构优化**
   - 建议按功能域进一步细分包结构
   - 增加版本管理，便于各业务模块按需引入

### 4.3 性能优化
1. **实体类优化**
   - 建议为大型实体类增加Builder模式
   - 对频繁使用的DTO增加缓存机制

2. **校验优化**
   - 建议增加分组校验（Group Validation）
   - 增加自定义校验器

## 五、功能扩展建议

### 5.1 新增功能
1. **增加审计字段支持**
   - 创建时间、更新时间、创建人、更新人
   - 建议提供BaseEntity基类

2. **增加多语言支持**
   - 国际化消息配置
   - 错误码国际化

3. **增加数据脱敏**
   - 手机号、邮箱等敏感信息脱敏
   - 提供脱敏注解

### 5.2 工具增强
1. **增加通用工具类**
   - 日期工具类
   - 字符串工具类
   - 集合工具类

2. **增加校验工具**
   - 身份证校验
   - 邮箱校验
   - 手机号校验

## 六、程序风险

### 6.1 高风险
1. **依赖传递风险**
   - 引入`spring-boot-starter-web`会被所有依赖此模块的业务模块继承
   - 可能导致网关模块（需要WebFlux）冲突

2. **全局异常处理风险**
   - 如果多个模块都有GlobalExceptionHandler，可能产生冲突
   - 建议移至网关层或配置中心

### 6.2 中风险
1. **版本兼容性**
   - MyBatis-Plus版本需与Spring Boot 3.x保持兼容
   - Lombok版本需与Java 21兼容

2. **序列化风险**
   - DTO/VO的serialVersionUID需要维护
   - 字段变更可能影响反序列化

### 6.3 低风险
1. **包命名规范**
   - 部分包名使用复数形式，建议统一
   - 常量类命名建议增加Const后缀

## 七、最佳实践

### 7.1 DTO设计原则
1. 按接口设计DTO，不共用
2. 增加完整的参数校验注解
3. 实现Serializable接口
4. 使用@Data简化代码

### 7.2 VO设计原则
1. 仅暴露必要字段
2. 敏感信息脱敏处理
3. 增加字段说明注释
4. 使用Builder模式

### 7.3 异常处理原则
1. 业务异常使用自定义异常
2. 增加异常码便于追踪
3. 日志记录完整上下文
4. 返回用户友好提示

## 八、维护建议

1. **定期更新依赖版本**
   - Spring Boot 3.x LTS版本
   - MyBatis-Plus最新稳定版
   - Lombok最新版本

2. **增加单元测试**
   - 转换器测试
   - 工具类测试
   - 异常处理测试

3. **文档维护**
   - 增加API文档
   - 更新变更日志
   - 维护使用示例
