# 单实例下 Semaphore 用于并发保护与 JVM 的关系，以及后续升级 Redis 分布式锁浅析

## 1. 本文要解决的问题

`diary-ai` 的 AI 调用是耗时且通常受第三方模型配额限制的操作。即使 RocketMQ 同时把多条消息推给一个应用实例，也不能让该实例无限并发地调用模型；否则会造成模型服务 `429`、HTTP 连接耗尽、线程堆积，甚至拖垮本实例。

当前项目使用 `LocalAiConcurrencyGuard` 中的 `Semaphore` 作为**单 JVM 的并发舱壁（bulkhead）**。它解决的是“这台 Worker 同时能执行多少个模型调用”，而不是“分布式环境中某一条任务只能由谁执行”。后者由数据库任务抢占、租约和版本号负责。

## 2. 当前 diary-ai 中的实现

相关代码：

- `diary.diaryai.guard.LocalAiConcurrencyGuard`
- `diary.diaryai.outbox.AiOutboxConsumer#consume`
- `diary.ai.limit.model-local-concurrency`
- `diary.ai.limit.local-permit-wait-ms`

核心实现如下：

```java
this.semaphore = new Semaphore(
        properties.getLimit().getModelLocalConcurrency(),
        true
);

public boolean tryAcquire() {
    try {
        return semaphore.tryAcquire(waitMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
    }
}
```

Consumer 收到消息后的关键顺序是：

```text
收到 RocketMQ 消息
        ↓
tryAcquire：申请本机模型调用许可证
        ↓
没有许可证（等待 waitMs 后仍失败）
        └── 返回 FAILURE，交给 RocketMQ 后续重投
        ↓
拿到许可证
        ↓
claimForExecution：数据库条件更新抢占该 AI 任务
        ↓
抢占成功后调用模型、续租、持久化结果
        ↓
finally 中 release：归还本机许可证
```

以 Nacos 中的下列配置为例：

```yaml
diary:
  ai:
    limit:
      model-local-concurrency: 2
      local-permit-wait-ms: 1000
```

同一 `diary-ai` 实例在任意时刻最多有两个任务进入模型执行区；第三个任务最多等 1 秒。如果仍没有空位，Consumer 返回 `ConsumeResult.FAILURE`，消息不会被静默确认。

## 3. Semaphore 与 JVM 的关系

### 3.1 为什么它只能保护单实例

`Semaphore` 是 JDK 的内存对象。`LocalAiConcurrencyGuard` 被 Spring 创建为 `@Component` 后，通常是当前 ApplicationContext 中的单例；它和内部的许可证计数都存在于**当前 JVM 的堆内存**中。

因此，同一个 JVM 内的多个 RocketMQ 消费线程共享同一个信号量：

```text
同一台 Worker / 同一 JVM

消费线程 1 ─┐
消费线程 2 ─┼── 同一个 Semaphore(2) ── 最多两个模型调用
消费线程 3 ─┘
```

但多个 JVM 的内存彼此隔离：

```text
Worker A（JVM A）：Semaphore(2)
Worker B（JVM B）：Semaphore(2)
Worker C（JVM C）：Semaphore(2)

集群最高模型并发约为 2 + 2 + 2 = 6
```

所以，`Semaphore` 不会跨机器、跨 Docker 容器、跨 Kubernetes Pod，也不会跨服务进程共享状态。扩容 `diary-ai` 时，每增加一个 Worker，都会额外获得该实例配置数量的本地许可证。

### 3.2 它与数据库抢占的职责不同

| 机制 | 解决的问题 | 作用范围 |
|---|---|---|
| `Semaphore` | 一台 Worker 的模型调用并发不能过高 | 单 JVM |
| RocketMQ Consumer Group | 同组实例协作分摊消息 | 一个消费组 |
| `claimForExecution` 条件更新 | 同一任务最终只能由一个 Worker 持有并执行 | 多实例共享数据库 |
| `workerId + leaseUntil + versionId` | Worker 宕机可接管；旧 Worker 不能覆盖新结果 | 多实例共享数据库 |

不能用本地 `Semaphore` 替代数据库抢占：两台机器各自都可能拿到本机许可证。也不应为了“单任务只能执行一次”把数据库条件更新替换为普通 Redis 锁；当前数据库状态机才是任务事实源和最终裁决者。

## 4. 什么是信号量许可证

`Semaphore(n)` 可以理解为初始化时有 `n` 张许可证：

```text
Semaphore(2)

初始：许可证 2
任务 A acquire → 许可证 1
任务 B acquire → 许可证 0
任务 C acquire → 等待或失败
任务 A release → 许可证 1
任务 C 获得许可证 → 许可证 0
```

它与 `synchronized` / `ReentrantLock` 的差异是：

- 锁通常只有一个持有者，适合保护一个临界区；
- 信号量可允许多个持有者，适合表达“有限数量的资源”；
- `Semaphore(1)` 可以实现互斥，但语义上更适合称为二元信号量；
- 信号量不记录许可证归属。理论上线程 A 获取、线程 B 也能 `release()`，因此业务代码必须严格配对。

当前 Consumer 的写法是正确的：只有 `tryAcquire()` 成功后才会进入后续 `try`，并在 `finally` 中 `release()`。获取失败直接返回，不会错误地归还许可证。

## 5. 公平信号量与非公平信号量

构造方法第二个参数控制公平性：

```java
new Semaphore(2, true);  // 公平
new Semaphore(2, false); // 非公平，默认值
```

### 5.1 公平信号量

当前项目使用 `true`。当多个线程因许可证不足而等待时，公平信号量会尽量按照等待顺序分配许可证，减少长期等待线程被后来任务反复插队的可能。

优点：

- 对长时间等待的任务更友好；
- 更容易解释和排查；
- 适合模型调用这类执行时间较长、数量较少的任务。

代价：

- 维护等待队列会有少量吞吐损耗；
- 公平不等于严格的业务完成顺序，线程调度和网络仍可能改变实际完成先后。

当前使用的是带超时的 `tryAcquire(waitMs, unit)`；它会参与公平排队。需注意，JDK 中**无超时的** `tryAcquire()` 为追求性能可能直接抢占可用许可证，并不保证遵守公平顺序。

### 5.2 非公平信号量

非公平模式下，新到达线程发现有许可证时可以直接获取，可能绕过已等待的线程。

优点：

- 吞吐量通常略高；
- 适合请求极短、量极大、允许个别任务等待更久的场景。

风险：

- 在高负载下，等待较久的任务可能饥饿；
- 排查“为什么某任务一直没有执行”更困难。

对当前 AI 调用场景，公平模式是合理的默认选择；模型调用远比这点排队开销昂贵。

## 6. Semaphore 的其他常见用法

| 用法 | 许可证含义 | 典型示例 |
|---|---|---|
| 有限资源池 | 可用资源数量 | 数据库连接、HTTP 连接、GPU 卡 |
| 外部服务并发保护 | 供应商允许的并发请求数 | LLM、支付、短信、地图服务 |
| 批处理舱壁 | 允许并行的任务槽位 | 图片转码、文件解析、报表生成 |
| 二元信号量 | 只有 1 个执行权 | 互斥访问某段本地代码 |
| 有界生产/消费 | 缓冲区剩余位置或已有元素 | 自定义阻塞队列（实际优先使用 `BlockingQueue`） |

在业务开发中，`Semaphore` 最常见的价值不是“加锁”，而是**限量使用稀缺资源**。`diary-ai` 的模型并发就是典型例子。

## 7. 当前实现需要意识到的行为

### 7.1 并发已满时返回 FAILURE

当前策略是等待 `localPermitWaitMs`，仍无法获取许可证就返回 RocketMQ `FAILURE`。优点是 Consumer 线程不会无限阻塞，消息也不会丢失。

但它并非没有成本：如果 Worker 长期满载，消息会反复消费失败，可能累计 RocketMQ 重试次数，最终进入 DLQ。也就是说，“本机繁忙”被表达成了“本次消息消费失败”。

后续可根据压测结果选择：

1. 增加 Worker 实例或适度提高 `model-local-concurrency`；
2. 让 Consumer 拉取并发与本地许可证数量更接近；
3. 对容量不足使用更明确的延迟重试/退避策略，而不是高频立即重试；
4. 监控消息堆积、重试次数、DLQ 数量和模型供应商 `429`。

### 7.2 InterruptedException 的处理

`tryAcquire` 被中断后会：

```java
Thread.currentThread().interrupt();
return false;
```

恢复中断标志是正确做法：它让上层线程池或框架仍能感知取消/关闭信号；随后返回 `false`，由 Consumer 走失败处理，不会继续发起新的模型调用。

### 7.3 不要重复 release

`Semaphore.release()` 不会校验“当前线程是否真的拥有许可证”。多调用一次会凭空增加许可证，导致并发上限失效。因此必须遵循：

```java
boolean acquired = semaphore.tryAcquire(...);
if (!acquired) {
    return;
}
try {
    // 使用资源
} finally {
    semaphore.release();
}
```

## 8. 什么时候需要从本地 Semaphore 升级到 Redis

仅仅部署多个 `diary-ai` 实例，不意味着必须迁移 Redis。当前模式的集群并发是“每实例上限 × 实例数”，很适合按 Worker 数量线性扩容。

当出现下列需求时，才考虑 Redis：

- 模型供应商限制的是**整个集群**最多 10 个并发请求；
- 多个不同服务共用同一模型账号或 GPU 资源；
- 希望无论扩容多少 Worker，营养分析的全局并发都固定为 10；
- 需要在运行时统一调整全局并发上限。

此时需求的本质不是“分布式互斥锁”，而通常是**分布式信号量**。如果模型全局允许并发 10 个，使用互斥锁只允许 1 个请求，会严重浪费容量。

## 9. Redis 分布式锁与分布式信号量的选择

| 业务目标 | 更适合的机制 | 容量 |
|---|---|---|
| 全局只允许一个实例执行一次维护任务 | Redis 分布式锁 | 1 |
| 同一 `taskId` 避免短期重复进入某段逻辑 | 带租约和 token 的 Redis 锁，但任务表 CAS 仍为最终保障 | 1 |
| 全集群最多 10 个模型调用 | Redis 分布式信号量 / Permit | N |
| 当前 AI 任务最终归属 | MySQL 条件更新 + `workerId` + 租约 + 版本号 | 每任务 1 |

对于当前 `ai_task`，不要把 `claimForExecution` 替换成 Redis 锁。Redis 锁可能因网络、过期、进程暂停而失效；数据库条件更新同时推进了业务状态，仍应保留为最终事实。

## 10. 若使用 Redis 分布式信号量：建议流程

假设模型账号要求“全体 Worker 合计最多 10 个并发调用”。可保留本地 Semaphore，再叠加 Redis 全局许可证：

```text
MQ 消息
  ↓
获取本地 Semaphore（保护本机线程、连接和内存）
  ↓
数据库 claimForExecution（确认任务归属）
  ↓
获取 Redis 全局 Permit（限制全体实例总模型并发）
  ↓
调用模型，并在调用期间保证 Permit 不会无故失效
  ↓
持久化结果
  ↓
释放 Redis Permit
  ↓
释放本地 Semaphore
```

### 10.1 Redis Permit 的实现要求

不要使用“先 `GET` 再 `SET`”等非原子组合操作。应选用以下之一：

- 成熟客户端实现，例如 Redisson 的 `RSemaphore` 或支持租约的 `RPermitExpirableSemaphore`；
- Redis Lua 脚本，将“检查剩余数 + 扣减 + 生成持有 token”做成一次原子操作；
- 明确的许可证记录与过期恢复策略。

一个健壮的 Permit 至少要包含：

```text
permitId / token：每次获取唯一，释放时验证身份
lease / ttl：实例崩溃后最终能够回收
renew：模型调用可能超过 ttl 时续租
原子 acquire / release：避免并发超卖
监控：可用许可证、持有者、等待和超时数量
```

如果调用模型可能持续 5 分钟，而 Redis Permit 的 TTL 是 30 秒，则必须在调用期间续租；否则 Permit 到期后会被另一个 Worker 获取，实际并发会超过设定上限。

### 10.2 获取失败时的处理

Redis Permit 获取失败意味着“集群整体已满”，不是任务已经处理失败。建议：

1. 不执行模型调用；
2. 不把任务错误地标记成业务失败；
3. 通过受控的延迟重试、状态机重入或 MQ 延迟消息稍后再试；
4. 确保当前 `workerId`、数据库租约和 Redis Permit 的释放/过期路径一致。

特别注意顺序：如果先成功 `claimForExecution`，随后 Redis Permit 获取失败，任务已是 `RUNNING`。此时要么继续持有任务租约并等待 Permit，要么将任务用条件更新退回 `RETRY_WAIT` 并投递重试事件；不能直接返回，否则任务可能一直停在 `RUNNING`。

## 11. 若使用 Redis 分布式锁：安全操作流程

Redis 锁只适合“同一时刻全局只能有一个执行者”的场景，例如单例清理任务。推荐使用 Redisson 的 `RLock`，由其处理 token、看门狗续期和安全释放。

若必须自行实现，最小安全流程是：

```text
1. 生成随机 token（UUID）
2. SET lockKey token NX PX leaseMillis，成功才拥有锁
3. 执行业务；长任务在安全条件下续期
4. 用 Lua 判断 value 仍等于自己的 token，再 DEL
5. 获取不到锁时，不执行临界业务；按业务需要稍后重试
```

释放锁不能直接 `DEL lockKey`：锁可能已过期且被其他实例重新取得；旧实例直接删除会误删新实例的锁。必须“比较 token 后删除”。

对于网络分区、GC Stop-The-World、Redis 主从切换等极端情况，分布式锁不能单独提供绝对业务正确性。因此任务结果写入仍应保留数据库版本校验（fencing / optimistic lock 思想）。

## 12. 建议的演进路线

1. **当前阶段**：保留本地公平 `Semaphore`，用 `model-local-concurrency` 控制每台 Worker 的模型并发；继续使用 MySQL `claimForExecution` 作为任务所有权权威。
2. **扩容阶段**：部署多个同 Group 的 Worker；总并发通过“实例数 × 本地并发”估算；确保 RocketMQ Topic Queue 数量足够。
3. **压测与监控阶段**：根据模型耗时、429、队列堆积、重试/DLQ、数据库连接池指标调整本地并发和实例数。
4. **需要全局配额时**：增加 Redis 分布式信号量，而不是把任务 CAS 替换成 Redis 锁；先明确 Permit 获取失败后的状态机和补偿路径。
5. **需要单例任务时**：单独为该维护任务使用带 token 与租约的 Redis 锁或成熟客户端，不要将它混入 AI 任务执行权模型。

## 13. 一句话总结

`Semaphore` 管的是“本 JVM 有多少个模型调用槽位”；数据库 CAS 管的是“这条任务最终归谁执行”；Redis 分布式信号量管的是“整个集群一共允许多少个调用”。三者可以协作，但不能互相简单替换。
