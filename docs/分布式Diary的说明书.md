# 分布式 Diary 的说明书

## 一、什么是分布式应用

分布式应用通常由多个独立进程或节点组成。节点之间不共享内存，而是通过网络通信，并共同完成一个完整的业务目标。各节点可能独立发生故障，因此系统还需要面对超时、重复消息、网络中断和部分成功等问题。

需要区分以下几个概念：

| 概念 | 含义 |
| --- | --- |
| Maven 多模块 | 代码和依赖的组织方式 |
| 模块可独立启动 | 具备拆成多个进程运行的能力 |
| 分布式应用 | 多个进程通过网络协作，共同完成业务 |
| 微服务架构 | 在分布式基础上，进一步要求明确的业务边界、独立数据和独立发布 |

因此，“多模块、可以独立启动”本身不能证明一个项目是分布式应用。即使所有服务部署在同一台机器，只要它们作为不同进程，通过 HTTP、MQ、Redis 等网络机制协作，就已经具有分布式系统的基本特征，但这不代表系统已经具备高可用或达到成熟微服务的标准。

## 二、Diary-Self 当前的定位

Diary-Self 已经不只是普通的多模块项目，而是具有真实分布式通信的微服务雏形。

项目中存在多个独立的 Spring Boot 运行进程，并已经使用或接入了以下组件：

- Spring Cloud Gateway：统一入口和 JWT 校验。
- Nacos：服务发现和配置管理。
- Redis：Token、缓存和限流等。
- RocketMQ：异步任务和消息通信。
- XXL-Job：分布式任务调度。
- Netty WebSocket：通知服务。
- Micrometer、Prometheus：监控指标基础。

AI 模块已经形成了完整的异步处理链路：

```text
提交 AI 请求
  → 创建 AI 任务和 Outbox
  → 发布 RocketMQ 消息
  → Consumer 消费消息
  → 执行 AI 任务
  → 更新任务结果
```

该链路还考虑了乐观锁、任务租约、重试、超时恢复和 Outbox 状态，因此是项目中分布式特征最完整的部分。

综合来看，Diary-Self 在逻辑上已经属于分布式应用，但业务边界、数据边界和工程体系还不够成熟。更准确的定位是：

> 基于 Spring Cloud 的分布式单体，正在向微服务架构演进。

`diary-config` 和 `diary-utils` 虽然存在启动类，但主要作为其他模块依赖的共享 Jar 使用，不应仅仅因为能够启动就被视为独立业务服务。真正的配置中心是 Nacos。

## 三、当前的主要差距

### 1. 数据所有权和服务边界不清晰

当前存在跨领域直接访问数据库表的情况，例如：

- AI 服务直接 JOIN `recipe` 表。
- file 服务直接 JOIN `time_card`、`time_category` 表。
- goal 服务直接 JOIN `user` 表。
- sysInfo 服务直接维护 `love_mood` 表。

这会导致一个服务修改表结构时，其他服务也可能被迫一起修改，无法真正独立演进。

接下来可以让每个真正的业务服务拥有独立的 Schema。在 MySQL 中，Schema 基本等同于 Database，例如：

```text
diary_user
diary_recipe
diary_ai
diary_file
diary_goal
diary_love
```

每个服务使用独立数据库账号，并且只允许访问自己的数据库。`gateway`、`common`、`utils` 等没有独立业务数据的模块不一定需要数据库。

服务之间不再跨库 JOIN，也不直接修改其他服务的数据，只共享业务 ID 和通信契约。

#### AI 查询 Recipe 的处理方式

不能简单地把所有数据库 JOIN 机械地替换成 HTTP 请求，需要按照业务场景选择方式。

同步查询适用于必须立即获得最新数据的场景：

```text
AI 服务获得 recipeId
  → 调用 Recipe 服务接口
  → Recipe 服务查询自己的数据库
  → 返回 RecipeDTO
```

事件同步适用于读多且允许短暂数据延迟的场景。Recipe 服务在数据发生变化时发送创建、更新或删除事件，AI 服务订阅事件并维护自己需要的少量只读数据。

业务快照适用于 AI 异步任务。任务创建时保存菜谱名称、食材等输入快照，即使菜谱后来被修改，AI 任务仍然使用提交时的数据执行。AI Consumer 不应在执行到一半时强依赖 Recipe 服务在线。

列表查询还需要避免 N+1 网络请求。例如查询 50 条 AI 记录时，不应发送 50 次 Recipe 请求，而应使用批量查询接口或本地读模型。

### 2. `diary-common` 的范围过大

`diary-common` 当前包含 AI、user、recipe、goal、love、notify 等多个领域的实体和模型，会导致服务通过公共 Jar 形成编译期耦合。

可以把以下内容迁移回所属服务，由各服务独立扩展和维护：

- PO 和数据库实体。
- Mapper 相关模型。
- 业务 DTO、VO。
- 业务枚举和异常。
- 只被单个服务使用的工具和常量。

不一定要完全删除公共模块，可以保留一个很小的公共基础模块，仅包含统一响应结构、少量通用异常基类、Trace/Header 名称，以及真正没有业务含义的工具。

服务间共享的 HTTP 或 MQ 契约可以建立独立、版本化的 Contract 模块，但不能再次发展成巨型 Common，也不应在其中放置数据库 PO。

### 3. 网络调用治理不足

增加网络调用的目的不是增加调用数量，而是用明确的服务契约代替跨服务数据库访问。

同步网络调用需要考虑：

- 连接超时和读取超时。
- 熔断、限流和并发隔离。
- 降级策略。
- 有边界的重试。
- 幂等机制。
- API 版本和兼容性。
- Trace ID 透传。

查询请求通常可以进行有限重试；创建、扣减和删除等写请求不能盲目重试，必须先具备幂等机制。也应避免把一个请求拆成过长的同步调用链，以免发生级联故障。

通信方式可以按照以下原则选择：

```text
必须立即获得结果 → HTTP/RPC
允许最终一致性   → RocketMQ 事件
长耗时操作       → 异步任务
```

### 4. 部署和高可用能力不足

需要容器化的是每个真正独立部署的运行服务，例如 Gateway、User、Recipe、File、AI、Notify 和 Goal。`diary-common`、`diary-utils` 等 Library 不需要单独创建容器。

可以先为运行服务创建 Dockerfile，再使用 Docker Compose 一键启动完整环境：

```bash
docker compose up -d
```

Compose 中可以包含 Nacos、MySQL、Redis、RocketMQ、XXL-Job、Gateway、业务服务、Prometheus 和 Grafana。

推荐的建设顺序是：

```text
单服务 Docker
  → Docker Compose 完整环境
  → 多实例验证
  → CI/CD
  → 根据需要引入 Kubernetes
```

Kubernetes 可以用于多节点部署、弹性伸缩、滚动升级、服务自愈和配置管理，但不是判断一个应用是否为合格分布式应用的必要条件。

### 5. 可观测性没有形成完整闭环

需要追踪一次请求从发起到结束的完整链路，例如：

```text
前端
  → Gateway
  → AI Controller
  → Outbox
  → RocketMQ
  → AI Consumer
  → 模型调用
  → 数据库提交
```

同步和异步阶段应使用同一个 Trace ID，或建立明确的父子链路关系。

可观测性包括三类数据：

- 日志：记录发生了什么，并携带 `traceId`、`taskId`、`userId`。
- 指标：统计请求量、耗时、失败率和资源占用。
- 链路：定位时间具体消耗在哪个服务、数据库或消息阶段。

重点指标包括 HTTP 请求量和延迟、数据库连接池、JVM 内存和 GC、Redis 延迟、RocketMQ 堆积和消费延迟、Outbox 各状态数量，以及 AI 任务排队时间、执行时间和失败率。

### 6. 测试体系不完整

测试文档不能代替自动化测试，需要逐步建立：

```text
单元测试
  → 数据库、Redis、MQ 集成测试
  → 服务契约测试
  → 完整链路 E2E 测试
  → 故障和恢复测试
```

AI 模块尤其需要验证重复消费、Consumer 执行中宕机、消息发送成功但 Outbox 更新失败、RocketMQ 不可用、AI 调用超时、多个 Worker 抢占同一任务、租约过期恢复以及重试耗尽等场景。

### 7. 配置和权限需要治理

数据库密码、Nacos 地址和凭证、Redis 密码、RocketMQ 凭证、JWT 密钥、XXL-Job Access Token 和第三方 API Key 等配置应移出代码仓库，改为环境变量或密钥管理系统提供。

不同服务应使用不同的数据库账号，并遵循最小权限原则，例如：

```text
recipe_service_user → 只能访问 diary_recipe
ai_service_user     → 只能访问 diary_ai
xxljob_admin_user   → 只能访问 diary_xxl_job
```

开发、测试、预发布和生产环境还应分别维护自己的配置与密钥。

## 四、推荐的改造顺序

1. 明确真正的业务服务，将 `config`、`utils` 定位为普通 Library。
2. 明确每个服务的数据所有权，为业务服务划分独立 Schema 和数据库账号。
3. 优先拆除 AI 与 Recipe、Goal 与 User、File 与 TimeMachine 等跨服务 JOIN。
4. 瘦身 `diary-common`，将业务模型迁回所属服务。
5. 使用 HTTP、MQ 事件或业务快照建立明确的服务契约。
6. 为运行服务创建 Dockerfile，并通过 Docker Compose 建立可复现的完整环境。
7. 验证关键服务多实例运行时的幂等、调度、消费和故障恢复能力。
8. 补充调用链追踪、指标监控、容错机制和自动化测试。
9. 根据部署规模和实际需要决定是否引入 Kubernetes。

Diary-Self 的整体演进目标可以概括为：

> 从“多个服务共享代码和数据库”，演进为“每个服务拥有自己的业务模型和数据，通过明确的 HTTP 或 MQ 契约协作，并能够独立部署、扩容、故障和恢复”。
