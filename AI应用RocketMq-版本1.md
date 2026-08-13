# AI 应用 RocketMQ——版本 1

> 本图对应当前第一版代码实现，覆盖任务提交幂等、RocketMQ 投递、Consumer 原子抢占、任务执行、结果事务、失败重试、`attemptCount`、`versionId` 和租约控制。

```mermaid
flowchart TD
    START([客户端提交 AI 营养分析请求])

    subgraph SUBMIT[任务提交与幂等]
        S1[校验并规范化请求<br/>trim clientRequestId<br/>flag 转大写<br/>校验 Qwen Plus、DIET/RECIPE、materials]
        S2{按 userId + clientRequestId<br/>查询任务}
        S3[直接返回已有 taskId 和当前状态<br/>不重复发送 MQ]
        S4[序列化稳定 inputSnapshot]
        S5[创建 ai_task<br/>status=PENDING<br/>attemptCount=0<br/>versionId=0]
        S6{插入是否触发<br/>唯一索引冲突}
        S7[重新查询并发请求已创建的任务<br/>返回同一 taskId]
    end

    subgraph PRODUCER[Producer 与 RocketMQ]
        P1[构造任务消息<br/>eventId、taskId、userId、schemaVersion、traceId]
        P2[同步发送 Normal Message<br/>Message Key=taskId 字符串]
        P3{Broker 是否确认成功}
        P4[任务保持 PENDING<br/>finishTime 保持 NULL<br/>等待补偿或 Outbox]
        P5[条件更新 PENDING → QUEUED<br/>queueTime=首次发送成功时间<br/>versionId+1]
        P6{条件更新是否为 0}
        P7[Consumer 可能已先抢占<br/>重新读取实际状态<br/>不把 RUNNING/SUCCESS 覆盖回 QUEUED]
        P8[返回 taskId 和数据库实际状态]
    end

    subgraph CONSUMER[Consumer 协议校验与原子抢占]
        C1[接收 RocketMQ 消息]
        C2{消息体、taskId、taskType、<br/>schemaVersion 是否合法}
        C3[返回 FAILURE<br/>由 RocketMQ 有限重试并最终进入 DLQ]
        C4[生成本次唯一 workerId<br/>计算 leaseUntil]
        C5[原子抢占任务]
        C5D[一条 SQL 同时执行<br/>PENDING/QUEUED/RETRY_WAIT → RUNNING<br/>或接管租约已过期的 RUNNING<br/>attemptCount+1<br/>versionId+1<br/>写 workerId、leaseUntil、startTime]
        C6{抢占是否成功}
        C7[查询任务当前状态]
        C8{是否已是<br/>SUCCESS/FAILED 等终态}
        C9[重复消息直接 ACK<br/>不再次调用模型]
        C10{attemptCount 是否<br/>达到 maxAttempts}
        C11[租约过期且次数耗尽时<br/>条件更新为 FAILED<br/>versionId+1]
        C12[其他 Worker 持有有效租约<br/>当前重复消息 ACK]
    end

    subgraph EXECUTION[任务执行与 Worker 所有权]
        E1[Executor 再次读取 ai_task]
        E2{RUNNING、workerId、versionId<br/>是否仍与本次抢占一致}
        E3[已失去所有权<br/>停止执行，不调用模型]
        E4[从 inputSnapshot<br/>反序列化 AiInvokeDTO]
        E5[按 aiType 获取 InvokeQwenPlus]
        E6[构建单业务对象汇总 Prompt<br/>模型不返回 universalId]
        E7[调用 Qwen Plus]
        E8[解析并校验单个 JSON 对象<br/>检查六个营养字段]
    end

    subgraph RESULT[结果持久化本地事务]
        R1[插入 AiInfo]
        R2[插入 AiNutrient<br/>写入 aiTaskId、flag、universalId]
        R3[条件更新 RUNNING → SUCCESS<br/>校验 workerId + versionId<br/>写 aiInfoId、finishTime<br/>清理 Worker、租约和错误<br/>versionId+1]
        R4{三项操作是否都<br/>恰好影响一行}
        R5[提交本地事务<br/>任务完成]
        R6[回滚 AiInfo、AiNutrient<br/>和 SUCCESS 更新]
    end

    subgraph FAILURE[异常分类与重试]
        F1{是否为永久错误<br/>或 attemptCount 已耗尽}
        F2[条件更新 RUNNING → FAILED<br/>校验 workerId + versionId<br/>写错误与 finishTime<br/>versionId+1]
        F3[ACK<br/>业务失败不继续 MQ 重投]
        F4[条件更新 RUNNING → RETRY_WAIT<br/>校验 workerId + versionId<br/>记录错误并释放租约<br/>versionId+1]
        F5[返回 FAILURE<br/>RocketMQ 有限重投]
    end

    START --> S1 --> S2
    S2 -- 已存在 --> S3 --> END([结束])
    S2 -- 不存在 --> S4 --> S5 --> S6
    S6 -- 是 --> S7 --> END
    S6 -- 否 --> P1 --> P2 --> P3
    P3 -- 否 --> P4 --> END
    P3 -- 是 --> P5 --> P6
    P6 -- 是 --> P7 --> P8 --> END
    P6 -- 否 --> P8

    P2 -. Broker 投递 .-> C1
    C1 --> C2
    C2 -- 否 --> C3 --> END
    C2 -- 是 --> C4 --> C5 --> C5D --> C6
    C6 -- 否 --> C7 --> C8
    C8 -- 是 --> C9 --> END
    C8 -- 否 --> C10
    C10 -- 是 --> C11 --> END
    C10 -- 否 --> C12 --> END
    C6 -- 是 --> E1 --> E2
    E2 -- 否 --> E3 --> END
    E2 -- 是 --> E4 --> E5 --> E6 --> E7 --> E8 --> R1 --> R2 --> R3 --> R4
    R4 -- 是 --> R5 --> END
    R4 -- 否 --> R6 --> F1
    E4 -. 输入快照异常 .-> F1
    E7 -. 模型调用异常 .-> F1
    E8 -. 响应解析或校验异常 .-> F1
    F1 -- 是 --> F2 --> F3 --> END
    F1 -- 否 --> F4 --> F5
    F5 -. MQ 重投后重新抢占 .-> C1

    classDef startEnd fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20,stroke-width:2px;
    classDef decision fill:#fff8e1,stroke:#f9a825,color:#5d4037,stroke-width:1.5px;
    classDef failure fill:#ffebee,stroke:#c62828,color:#7f0000,stroke-width:1.5px;
    classDef success fill:#e3f2fd,stroke:#1565c0,color:#0d47a1,stroke-width:1.5px;
    classDef state fill:#f3e5f5,stroke:#7b1fa2,color:#4a148c,stroke-width:1.5px;

    class START,END startEnd;
    class S2,S6,P3,P6,C2,C6,C8,C10,E2,R4,F1 decision;
    class P4,C3,C11,R6,F2,F3,F4,F5 failure;
    class P5,P8,C5,C5D,E1,E4,E5,E6,E7,E8,R1,R2,R3,R5 success;
    class S5,C4,C7,C9,C12,E3 state;
```

## 核心字段变化

```mermaid
flowchart LR
    BEGIN([开始]) -->|创建任务<br/>attemptCount=0<br/>versionId=0| PENDING[PENDING<br/>等待消息确认]
    PENDING -->|Broker 确认<br/>versionId+1| QUEUED[QUEUED<br/>消息已入队]
    PENDING -->|消息先于 QUEUED 更新到达<br/>attemptCount+1 / versionId+1| RUNNING[RUNNING<br/>Worker 持有租约]
    QUEUED -->|Consumer 原子抢占<br/>attemptCount+1 / versionId+1| RUNNING
    RETRY[RETRY_WAIT<br/>等待 MQ 重投] -->|再次抢占<br/>attemptCount+1 / versionId+1| RUNNING
    RUNNING -->|可重试错误<br/>释放租约 / versionId+1| RETRY
    RUNNING -->|结果事务成功<br/>校验 workerId + versionId<br/>versionId+1| SUCCESS[SUCCESS<br/>结果已可靠保存]
    RUNNING -->|永久错误或次数耗尽<br/>finishTime / versionId+1| FAILED[FAILED<br/>业务执行失败]
    RUNNING -->|租约过期| RECOVER[允许新 Worker 接管]
    RECOVER -->|原子重新抢占<br/>attemptCount+1 / versionId+1| RUNNING
    SUCCESS --> FINISH([结束])
    FAILED --> FINISH
```

## 字段语义

- `attemptCount`：只在 Consumer 成功抢占、准备进入一次模型执行流程时由数据库递增；MQ 重复投递、协议解析失败和抢占失败均不递增。
- `versionId`：创建时为 `0`，每次有效状态迁移都由数据库执行 `version_id = version_id + 1`。
- `workerId + versionId`：共同证明当前线程仍拥有 RUNNING 任务；旧 Worker 失去租约后不能再提交 SUCCESS、FAILED 或 RETRY_WAIT。
- `leaseUntil`：Consumer 抢占时写入；离开 RUNNING 状态时清空，租约过期后允许其他 Worker 接管。
- `finishTime`：只在 SUCCESS 或 FAILED 终态写入，PENDING、QUEUED、RUNNING、RETRY_WAIT 均保持为空。
