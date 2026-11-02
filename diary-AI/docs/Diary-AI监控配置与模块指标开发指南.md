# Diary-AI 监控配置与模块指标开发指南

## 1. 目标与边界

本指南记录 `diary-AI` 已落地的 Prometheus + Grafana 监控方案，覆盖：

- Spring Boot / JVM / HTTP / HikariCP 等通用指标；
- AI 服务独有的本地并发、续租线程池、任务状态和 Outbox 状态；
- Prometheus 抓取与 Grafana 面板配置；
- 健康检查的 liveness / readiness 使用方式；
- 为一个业务模块定制指标时的设计原则。

本文不包含链路追踪、熔断、告警通知渠道或模型 SDK 调用超时；它们属于后续治理工作。

## 2. 整体数据流

```text
diary-AI
  ├─ Spring Boot Actuator
  ├─ Micrometer MeterRegistry
  │    ├─ JVM / HTTP / HikariCP 自动指标
  │    └─ Diary-AI 自定义业务指标
  └─ GET /actuator/prometheus
             │
             ▼
      Prometheus（周期抓取、保存时序数据）
             │
             ▼
      Grafana（PromQL 查询、仪表盘、后续告警）
```

Prometheus 只负责抓取和存储数值；Grafana 负责把 PromQL 查询结果展示成图表、仪表盘或告警。

## 3. 依赖与 Actuator 配置

### 3.1 Maven 依赖

`pom.xml` 需要以下依赖：

```xml
<!-- 暴露 Actuator 健康检查与指标端点 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- 将 Micrometer 指标序列化为 Prometheus 文本格式 -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

有这两个依赖后，Spring Boot 会自动注册大量通用指标，例如 JVM 内存、GC、线程、HTTP 请求和 HikariCP 连接池。

### 3.2 Nacos 配置源

本项目使用 [a](../src/main/resources/a) 作为同步到 Nacos `diary-AI.yaml` 的配置源。以下配置应位于 YAML 顶层的 `management`：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      probes:
        enabled: true
      # 对外只暴露状态，异常详情只记录在服务日志中。
      show-details: never
      group:
        readiness:
          # diary-AI 无法在 MySQL/Redis 不可用时可靠地受理任务。
          include: readinessState,db,redis
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
  metrics:
    tags:
      # 所有 Micrometer 指标附带同一应用标签，便于跨服务筛选。
      application: ${spring.application.name:diary-ai}
```

部署后可访问：

| 地址 | 用途 | 预期 |
| --- | --- | --- |
| `/actuator/prometheus` | Prometheus 文本指标 | `jvm_memory_used_bytes` 等文本 |
| `/actuator/health/liveness` | 进程是否仍存活 | `UP` |
| `/actuator/health/readiness` | 实例是否可接收流量，含 MySQL/Redis | `UP` |
| `/actuator/health` | 全部健康贡献者的汇总 | 仅用于诊断，不作为流量探针 |

> liveness 不应依赖 MySQL、Redis 等外部系统，否则短暂依赖故障会导致容器被无意义重启。readiness 则可以依赖它们：依赖故障时实例不接新流量，但进程保持运行，待依赖恢复后自动就绪。

### 3.3 业务指标快照周期

下面的配置在 `diary.ai.metrics` 下，与 `limit.global-model-permit` 并列，不能嵌入并发配额配置：

```yaml
diary:
  ai:
    limit:
      global-model-permit:
        # 模型全局并发配额配置
        # ...
    metrics:
      # 数据库聚合结果写入内存 Gauge 的周期，单位毫秒。
      snapshot-interval-ms: 15000
```

`AiTaskProperties.Metrics` 对该值做了 `@Min(1000)` 校验。监控配置与限流配置分开建模，避免调节监控频率时误改变模型配额。

## 4. Prometheus 配置

Prometheus 需要抓取 `diary-AI` 暴露的端点。示例：

```yaml
scrape_configs:
  - job_name: diary-ai
    scrape_interval: 5s
    scrape_timeout: 5s
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - AI_SERVICE_HOST:8807
        labels:
          application: diary-ai
```

在 Prometheus 查询页执行：

```promql
up{job="diary-ai"}
```

返回 `1` 表示 Prometheus 可以在超时前访问端点并解析指标；返回 `0` 表示网络、端口、端点、服务状态或抓取超时存在问题。`up=1` 只代表“监控抓取成功”，不等同于所有业务依赖都健康，业务接流量应以 readiness 为准。

## 5. 为什么模块需要定制监控

Micrometer 会自动观测 JVM、Web 和 HikariCP，但它不了解业务含义。例如：

- JVM 线程数无法说明 AI 模型调用额度是否耗尽；
- HTTP QPS 无法说明任务是否在 `RETRY_WAIT` 堆积；
- 数据库连接池正常也无法说明 Outbox 是否已经进入 `DEAD`。

因此，需要在模块的关键状态变化点旁注册低基数业务指标。一个指标应回答一个明确问题：

| 业务问题 | 指标 |
| --- | --- |
| 本实例还能同时调用多少个模型？ | `diary_ai_concurrency_available` |
| 是否因本地并发满而拒绝消费？ | `diary_ai_concurrency_rejected_total` |
| 续租调度器是否积压？ | `executor_queued_tasks{name="diary.ai.lease-renewer"}` |
| 任务是否进入重试或死信？ | `diary_ai_task_status_count{status="..."}` |
| Outbox 是否投递失败并耗尽重试？ | `diary_ai_outbox_status_count{status="DEAD"}` |

### 5.1 指标命名与标签原则

- Java 侧采用点分名称，例如 `diary.ai.concurrency.available`；Prometheus 端会显示为下划线名称 `diary_ai_concurrency_available`。
- Counter 在 Prometheus 中通常以 `_total` 结尾，例如 `diary_ai_concurrency_rejected_total`。
- `status` 是固定枚举标签，可安全使用。
- **禁止**使用 `taskId`、`userId`、`clientRequestId`、完整 URL 参数等动态值作为标签。每一种不同标签组合都会产生新的时序；高基数会显著拖慢并耗尽 Prometheus。

## 6. 已实现的定制指标

### 6.1 续租线程池

文件：[AiTaskLeaseRenewer.java](../src/main/java/diary/diaryai/guard/AiTaskLeaseRenewer.java)

AI 任务执行时间可能长于一次消费周期，`AiTaskLeaseRenewer` 使用单线程调度器定期续租。该执行器不是 Spring 自动创建的线程池，因此要显式绑定：

```java
ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, threadFactory);
executor.setRemoveOnCancelPolicy(true);

this.scheduler = ExecutorServiceMetrics.monitor(
        meterRegistry,
        executor,
        "diary.ai.lease-renewer"
);
```

这会产生标准 Micrometer 线程池指标：

```promql
executor_active_threads{job="diary-ai", name="diary.ai.lease-renewer"}
executor_queued_tasks{job="diary-ai", name="diary.ai.lease-renewer"}
executor_completed_tasks_total{job="diary-ai", name="diary.ai.lease-renewer"}
```

选择 `ScheduledThreadPoolExecutor` 而不是被 JDK 包装的通用 `ExecutorService`，可以让 Micrometer 更稳定地读取活动线程与队列信息。`setRemoveOnCancelPolicy(true)` 使已完成任务取消续租后立即离开队列。

> 当前自定义指标覆盖的是“AI 任务租约续期线程池”。RocketMQ 客户端内部消费线程池由框架管理，整体规模可通过 `jvm_threads_live_threads` 观察；若未来自行创建 RocketMQ 业务执行器，也应使用相同方式显式绑定。

### 6.2 本地 AI 并发 Semaphore

文件：[LocalAiConcurrencyGuard.java](../src/main/java/diary/diaryai/guard/LocalAiConcurrencyGuard.java)

`Semaphore` 是进程内对象，Spring Boot 不会自动为它产生监控指标。代码使用 `Gauge` 和 `Counter` 注册：

```java
Gauge.builder("diary.ai.concurrency.available", semaphore, Semaphore::availablePermits)
     .register(meterRegistry);

Gauge.builder("diary.ai.concurrency.max", () -> maxConcurrency)
     .register(meterRegistry);

Counter rejected = Counter.builder("diary.ai.concurrency.rejected")
                          .register(meterRegistry);
```

含义如下：

| 指标 | 类型 | 含义 |
| --- | --- | --- |
| `diary_ai_concurrency_available` | Gauge | 当前实例剩余模型调用许可 |
| `diary_ai_concurrency_max` | Gauge | 当前实例配置的许可上限 |
| `diary_ai_concurrency_rejected_total` | Counter | 等待许可超时、消费者返回失败的累计次数 |
| `diary_ai_concurrency_interrupted_total` | Counter | 等待许可时线程被中断的累计次数 |

本地 Semaphore 不是跨实例全局限流。多副本场景下，应结合 Redis 全局模型许可配置控制供应商总并发。

### 6.3 任务与 Outbox 状态快照

相关文件：

- [AiOperationalMetrics.java](../src/main/java/diary/diaryai/metrics/AiOperationalMetrics.java)
- [StatusCount.java](../src/main/java/diary/diaryai/metrics/StatusCount.java)
- [DiaryAiMapper.java](../src/main/java/diary/diaryai/mapper/DiaryAiMapper.java)
- [DiaryAIMapper.xml](../src/main/resources/mapper/DiaryAIMapper.xml)

任务状态和 Outbox 状态存储在 MySQL。一个错误方案是让 Prometheus 每次抓取 `/actuator/prometheus` 时直接执行 `GROUP BY status`；多个 Prometheus 或频繁抓取会把监控流量直接变成数据库压力。

当前实现采用以下方式：

```text
ApplicationReadyEvent：立即查询一次数据库
        ↓
@Scheduled：每 15 秒执行一次 GROUP BY status
        ↓
查询结果写入 AtomicLong 内存快照
        ↓
Gauge 只读取 AtomicLong
        ↓
Prometheus 任意频率抓取都不访问数据库
```

核心思想：

```java
// 每个固定 status 在启动时注册一个 Gauge；后续仅更新 AtomicLong 数值。
Gauge.builder(metricName, statusCounter, AtomicLong::get)
     .tag("status", status)
     .register(meterRegistry);

// 数据库暂时不可用时保留上一次成功快照，不把全部数据误报为 0。
try {
    refreshStatusCountsFromDatabase();
} catch (RuntimeException exception) {
    log.warn("刷新 AI 运营指标快照失败", exception);
}
```

得到的指标：

```promql
diary_ai_task_status_count{job="diary-ai", status="RUNNING"}
diary_ai_task_status_count{job="diary-ai", status="RETRY_WAIT"}
diary_ai_task_status_count{job="diary-ai", status="DEAD_LETTER"}

diary_ai_outbox_status_count{job="diary-ai", status="NEW"}
diary_ai_outbox_status_count{job="diary-ai", status="RETRY_WAIT"}
diary_ai_outbox_status_count{job="diary-ai", status="DEAD"}
```

## 7. Grafana 看板

建议创建 `Diary AI Operations` 看板。已有的 `JVM (Micrometer)` 和 `Spring Boot Http (3.x)` 可复用；AI 看板专注于业务状态。

| 面板 | PromQL | 推荐类型 | 正常现象 |
| --- | --- | --- | --- |
| 抓取状态 | `up{job="diary-ai"}` | Stat | 值为 1 |
| AI 可用并发许可 | `diary_ai_concurrency_available{job="diary-ai"}` | Stat | 空闲时等于配置上限 |
| 近 5 分钟并发拒绝 | `sum(increase(diary_ai_concurrency_rejected_total{job="diary-ai"}[5m]))` | Stat | 值为 0 |
| AI 任务状态 | `sum by (status) (diary_ai_task_status_count{job="diary-ai"})` | Bar gauge | `RETRY_WAIT` 不持续增长 |
| Outbox 状态 | `sum by (status) (diary_ai_outbox_status_count{job="diary-ai"})` | Bar gauge | `DEAD` 为 0 |
| 续租线程池队列 | `executor_queued_tasks{job="diary-ai", name="diary.ai.lease-renewer"}` | Time series | 接近 0 |
| AI 并发使用率 | `100 * (1 - diary_ai_concurrency_available{job="diary-ai"} / diary_ai_concurrency_max{job="diary-ai"})` | Gauge | 常态远低于 100% |
| 死信数量 | `sum(diary_ai_outbox_status_count{job="diary-ai", status="DEAD"})` | Stat | 值为 0 |
| 重试积压 | `sum(diary_ai_task_status_count{job="diary-ai", status="RETRY_WAIT"})` | Stat | 值为 0 或短暂波动 |
| JVM 存活线程 | `jvm_threads_live_threads{job="diary-ai"}` | Time series | 无异常持续增长 |
| 请求 QPS | `sum(rate(http_server_requests_seconds_count{job="diary-ai"}[1m]))` | Time series | 与访问量相符 |
| 近 5 分钟请求量 | `sum(increase(http_server_requests_seconds_count{job="diary-ai"}[5m]))` | Stat | 与当前访问热度相符 |
| Top API 请求 | `topk(10, sum by (method, uri) (increase(http_server_requests_seconds_count{job="diary-ai", uri!~"/actuator.*"}[5m])))` | Bar gauge | URI 应是路由模板而非真实 ID |

`increase(...[5m])` 是“最近 5 分钟新增量”，适合 Counter。它不代表累计总值；5 分钟只是平滑窗口，可按需求改为 1 分钟或 15 分钟。

## 8. 部署后验证清单

1. 将 [a](../src/main/resources/a) 同步到 Nacos `diary-AI.yaml`。
2. 重新部署 `diary-AI`，确保新 Java 指标代码生效。
3. 验证 `/actuator/health/liveness` 与 `/actuator/health/readiness` 均为 `UP`。
4. 访问 `/actuator/prometheus`，搜索 `diary_ai_` 与 `executor_` 前缀。
5. 在 Prometheus `/targets` 页面确认 `diary-ai` 为 `UP`，或查询 `up{job="diary-ai"}` 得到 `1`。
6. 等待至少一个 `snapshot-interval-ms` 周期，查询任务与 Outbox 状态指标。
7. 提交一笔测试 AI 任务，观察 `PENDING/QUEUED → RUNNING → SUCCESS` 和 Outbox 状态变化。
8. 仅在测试环境把本地并发临时设为 1 并制造并发任务，验证 `available` 变为 0、`rejected_total` 增长。

## 9. 后续建议

- 为 `up == 0`、Outbox `DEAD > 0`、任务 `DEAD_LETTER > 0`、重试积压持续增长配置告警。
- 为任务提交、模型调用、Outbox 发送补充成功/失败耗时 Timer 和 Counter。
- 接入 OpenTelemetry + Tempo/Jaeger，将 HTTP、Feign、RocketMQ、模型调用的 Trace ID 串联。
- 根 `/actuator/health` 若持续 `DOWN`，应通过受保护的诊断环境定位具体 HealthContributor；生产流量探针继续使用 liveness/readiness，不应简单忽略该问题。
