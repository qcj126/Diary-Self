# 不推倒重来：diary-AI 多场景任务能力的拆分与落地

> 文档性质：架构演进复盘 + 后续扩展导航  
> 当前基线：2026-10-23  
> 适用范围：`diary-AI` 模块中，以 `ai_task` 为中心的异步 AI 任务能力。  
> 维护原则：本文描述的是当前实现和已经作出的设计取舍；后续增加任务时，必须同步更新本文的“当前能力矩阵”“代码导航”“扩展步骤”和“变更记录”。

---

## 给后续开发者与 AI 的阅读导航

如果你是在新的对话或新的开发窗口中阅读本文，请按下面顺序理解和维护系统，不要只根据枚举名或单个 Mapper 猜测架构。

1. 先阅读 [当前设计结论](#当前设计结论) 和 [当前能力矩阵](#当前能力矩阵)，确认已有任务、应用和结果类型。
2. 再阅读 [代码导航](#代码导航)，从入口、任务创建、异步执行、结果落库、查询五条路径定位代码。
3. 新增能力前，先完成 [新增任务或场景的标准步骤](#新增任务或场景的标准步骤) 中的设计判断；不要直接给现有营养 SQL 增加一个 `JOIN`。
4. 完成代码后，按 [更新本文的规则](#更新本文的规则) 回填文档。文档中的“当前状态”必须以当前源码、DDL、配置和接口文档为准；发现不一致时，应先修正文档或在变更记录中明确差异。

### 更新本文的规则

每次新增或删除 AI 任务、变更任务语义、调整结果结构、修改 MQ 路由或改变接口契约时，至少更新以下位置：

- [当前能力矩阵](#当前能力矩阵)：新增任务对应的 `aiApplication`、`taskType`、`targetType`、输入快照和结果类型。
- [代码导航](#代码导航)：新增入口、处理器、结果 VO/Mapper 或配置类。
- [新增任务或场景的标准步骤](#新增任务或场景的标准步骤)：若本次发现了新的通用步骤或例外，补充进去。
- [变更记录](#变更记录)：记录日期、变更内容、影响面和验证方式。
- 根目录 [前端调用后端参数文档.md](../前端调用后端参数文档.md)：任何前端可调用接口、字段或状态语义变化都必须同步。

更新时应遵守两条底线：

- 不把“计划中的枚举值”写成“已经可用的任务”。是否可用取决于入口、任务创建、执行分派、结果落库、查询接口和测试是否全部存在。
- 不把历史讨论中的方案当作当前实现。本文的“演进过程”用于解释决策；“当前设计结论”和当前源码才是行为依据。

---

## 一、背景：最初只有食谱营养分析

最初 `diary-AI` 只服务一个场景：对食谱的食材进行营养分析，展示食谱标题和卡路里、蛋白质、脂肪等结果。这个场景很容易形成一条看似直接、实则脆弱的固定链路：

```text
ai_task
  -> ai_info
    -> ai_nutrient
      -> recipe.title
```

在只有“食谱营养分析”时，这种链路能工作；但它隐含了几个并不成立的前提：

- 每个任务都是营养任务；
- 每个结果都应该写入 `ai_nutrient`；
- 每个任务的目标都能关联 `recipe`；
- 每个结果都能用营养 VO 表达；
- 列表和详情都可以依赖业务表实时 `JOIN`。

一旦接入饮食营养分析、恋爱记录情感分析，甚至将来的冲突复盘、沟通建议，这些前提就会逐个失效。

---

## 二、原始困境：不是枚举数量不够，而是职责被绑死了

### 2.1 任务、目标对象、结果、查询 DTO 被绑成一条固定链

旧的通用查询把任务、模型调用信息、营养结果和食谱表放在一条 SQL 中。这样会出现：

- 饮食营养任务即使有结果，也可能因为不关联 `recipe` 而查询不到；
- 情感任务即使执行成功，营养字段也无法承载 `sentiment`、`score`、`summary`、`suggestion`；
- 任务尚未完成、失败或尚未产生结果时，`INNER JOIN` 会把任务从列表中“吞掉”；
- 每增加一种结果类型，都要继续给同一个大查询和同一个 `AiTaskResultVo` 打补丁。

真正需要拆开的不是 `aiApplication` 和 `taskType` 的数量，而是下面四个责任：

```text
任务是什么            -> ai_task.task_type
它属于哪个产品能力     -> ai_task.ai_application / ai_info.ai_application
它分析谁               -> ai_task.target_type / target_id / target_title
它产出了什么           -> 对应结果表与专属结果 VO
```

### 2.2 为什么不直接重写整个 diary-AI

测试阶段允许调整旧表和旧数据，不等于应该一次性推倒重来。完整引入通用 Handler 注册中心、目标解析器、通用结果协议等架构，理论上更泛化，但改动会同时触及 Controller、DTO、事务服务、MQ、策略、Mapper、表结构和前端接口，学习和排错成本过高。

本次选择的是**渐进式拆分**：保留已经可用的任务状态机、Outbox、MQ、重试和模型策略框架；仅在必要的边界上拆出营养与情感两条链路。

---

## 三、当前设计结论

### 3.1 `ai_task` 是稳定的任务中心

无论任务是营养还是情感，首先都是一条 `ai_task`。它保存跨任务类型都成立的信息：

- 任务归属：`userId`；
- 幂等信息：`clientRequestId`、`requestHash`；
- 执行语义：`taskType`、状态、尝试次数、租约、乐观锁版本；
- 目标对象：`targetType`、`targetId`、`targetTitle`；
- 稳定输入：`inputSnapshot`；
- 执行时间、错误码和错误信息。

其中 `targetTitle` 是提交时写入任务的展示快照。任务历史列表不再依赖业务数据源仍然存在、标题仍然未变，才能显示“当时分析了什么”。

### 3.2 `aiApplication`、`taskType`、`targetType` 的分工

| 字段 | 回答的问题 | 当前定位 |
|---|---|---|
| `aiApplication` | 这是哪一类产品能力？ | 用于分类、统计和页面维度；由服务端固定，不信任前端传入。 |
| `taskType` | 这是哪一个可执行的 AI 用例？ | 决定任务执行分派、结果查询和接口类型。 |
| `targetType` | 当前任务分析的目标/场景是什么？ | 与 `targetId` 一起定位业务对象或业务场景。 |
| `targetId` | 目标对象的主键是什么？ | 例如食谱、饮食记录、恋爱记录 ID。 |
| `targetTitle` | 用户当时提交的是哪一项？ | 列表和结果展示使用的提交时标题快照。 |

当前实现采用“任务场景明确化”的语义：简单任务中，`targetType` 可以是 `RECIPE`、`DIET`、`LOVE_RECORD`；未来如果“冲突复盘”本身就是一个独立分析场景，也可以使用：

```text
taskType   = LOVE_RECORD_CONFLICT_REFLECTION
targetType = LOVE_RECORD_CONFLICT_REFLECTION
targetId   = 对应恋爱记录 ID
```

这不是错误的重复，而是明确表示“本任务分析的是一条用于冲突复盘的恋爱记录”。前提是团队持续把 `targetType` 解释为**分析目标/场景类型**，而不是狭义的数据库表名。

### 3.3 当前能力矩阵

| AI 应用 | 编码 | 任务类型 | 目标类型 | 主要输入快照 | 结果表 | 专属结果接口 |
|---|---:|---|---|---|---|---|
| 营养分析 | `1` | `RECIPE_NUTRIENT_ANALYSIS` | `RECIPE` | `AiInvokeDTO` 的规范化表单 | `ai_nutrient` | 营养状态、营养结果 |
| 营养分析 | `1` | `DIET_NUTRIENT_ANALYSIS` | `DIET` | `AiInvokeDTO` 的规范化表单 | `ai_nutrient` | 营养状态、营养结果 |
| 感情分析 | `2` | `LOVE_RECORD_SENTIMENT_ANALYSIS` | `LOVE_RECORD` | `LoveRecordSentimentInputSnapshotDTO` | `ai_sentiment` | 情感状态、情感结果 |

`AiApplicationEnum` 中已存在更多规划方向，不代表它们已经是可执行任务。新增应用或任务时必须走完整链路，不能只增加一个枚举值。

---

## 四、渐进式改造过程

### 阶段一：从“只关联食谱”改为“任务记录目标对象”

**遇到的问题**：营养结果和食谱表强关联，导致饮食营养分析没有位置；查询 VO 还暴露旧的 `universalId/flag`，而不是任务中心的目标信息。

**改造方式**：

- 在 `ai_task` 中保存 `targetType`、`targetId`、`targetTitle`；
- 营养提交时根据 `RECIPE` 或 `DIET` 通过 Feign 查询当前用户自己的目标标题/名称；
- 食谱和饮食仍共用 `ai_nutrient`，因为它们的结果结构同为营养数据；
- 营养结果 VO 统一从任务表返回目标信息，而不再依赖 `ai_nutrient.universal_id/flag`。

**得到的结论**：数据来源不同不必然意味着结果表不同；结果语义相同就可以复用结果表，任务目标信息由 `ai_task` 统一保存。

### 阶段二：把 AI 应用类型收回服务端

**遇到的问题**：如果前端提交 `aiApplication`，调用者可以把目标类型与应用类型混搭；而任务创建、模型调用信息和 Outbox 又可能写出不同的应用编码。

**改造方式**：

- 营养入口内部固定 `AiApplicationEnum.NUTRITION_ANALYSIS`；
- 恋爱情感入口内部固定 `AiApplicationEnum.SENTIMENT_ANALYSIS`；
- 前端只提交业务必需的输入、目标 ID、模型类型和 `clientRequestId`，不决定系统内部的应用分类；
- 任务创建事务统一把确定后的应用编码写入任务链路和模型调用信息。

**得到的结论**：`aiApplication` 是服务端的业务定义，不是客户端可自由组合的参数。

### 阶段三：为恋爱记录建立独立情感分析链路

**遇到的问题**：情感结果不是营养数据；若仍让所有任务调用 `processNutrientData(...)`，情感任务会被错误写入营养表。

**改造方式**：

- 增加独立提交 DTO：`LoveRecordSentimentSubmitDTO`；
- 增加独立接口：`POST /ai/sentiment/task/love-record-analyze`；
- 通过 `LoveRecordClient` 获取经过用户归属校验的 `LoveRecordAiSnapshotDTO`；
- 以 `LoveRecordSentimentInputSnapshotDTO` 写入 `inputSnapshot`；
- 增加 `InvokeLoveRecordAIService`、情感模型策略和 `ai_sentiment`；
- 情感执行成功时调用 `processLoveRecordSentimentData(...)`，而营养仍调用 `processNutrientData(...)`。

第一版情感结果采用受控 JSON 契约：

```json
{
  "sentiment": "POSITIVE | NEUTRAL | NEGATIVE | MIXED",
  "score": "-100 到 100 的整数",
  "summary": "分析摘要",
  "suggestion": "温和且可执行的建议"
}
```

模型结果必须在策略层校验字段齐全、枚举合法、分数范围和文本长度后，才能进入事务落库。

### 阶段四：查询按“摘要 + 专属详情”拆开

**遇到的问题**：通用列表固定 `JOIN ai_nutrient` 并返回 `AiTaskResultVo`。情感任务出现在列表时没有对应字段；未完成、失败任务也可能消失。

**改造方式**：

- 通用列表改为 `AiTaskSummaryVo`，仅查询 `ai_task` 的公共任务字段；
- 营养详情返回 `AiTaskResultVo`；情感详情返回 `AiSentimentTaskResultVo`；
- 查询专属详情前先按 `taskType` 校验任务类型，避免将情感任务误走营养查询；
- 分页限制为 `pageSize` 1～100，`pageNum` 非负，并校验分页偏移量溢出。

**得到的结论**：列表负责“任务发生了什么”，详情负责“这个任务产出了什么”。不要试图让一个大而全的结果 VO 兼容所有业务结果。

### 阶段五：补齐异步可靠性与部署边界

保留并沿用原有可靠性设计：

- 同一事务写入任务和任务 Outbox；
- Outbox 发布器异步投递 RocketMQ；
- Consumer 通过任务状态、`workerId` 和 `versionId` 抢占执行权；
- 成功时同一事务写模型调用信息、对应结果、任务成功状态和完成事件 Outbox；
- 失败时按永久错误、可重试错误、重试耗尽区分状态；
- 恢复任务处理超时运行中的任务；
- `userId + clientRequestId` 的数据库唯一约束是幂等权威，`requestHash` 防止同一个幂等键提交不同内容。

---

## 五、当前结构与调用流程

### 5.1 模块结构图

```text
DiaryAIController
 ├─ 营养提交 / 查询接口
 ├─ 情感提交 / 查询接口
 └─ 通用任务摘要列表

应用服务
 ├─ AiTaskNutrientApplicationServiceImpl
 └─ AiTaskSentimentApplicationServiceImpl
      │
      ├─ 校验和规范化请求
      ├─ 校验幂等键与请求指纹
      ├─ 读取并校验目标对象
      └─ AiTransactionServiceImpl 创建 ai_task + mq_outbox
                                      │
                                      ▼
                              RocketMQ 任务消息
                                      │
                                      ▼
                              AiTaskExecutor
                               ├─ 营养分支
                               └─ 情感分支
                                      │
                                      ▼
                              AiTaskProcessor
                               ├─ AIFactory 营养模型缓存
                               └─ AIFactory 情感模型缓存
                                      │
                                      ▼
                         专属模型策略 + 输出校验
                                      │
                                      ▼
                       AiTransactionServiceImpl 落库
                        ├─ ai_info
                        ├─ ai_nutrient 或 ai_sentiment
                        ├─ ai_task -> SUCCESS
                        └─ 完成事件 mq_outbox
```

### 5.2 营养任务提交

```text
POST /ai/nutrient/task/nutrient-analyze
  -> AiTaskNutrientApplicationServiceImpl
  -> 规范化 flag（转大写）并校验 ingredients、模型、幂等键
  -> RECIPE: RecipeClient 查询标题；DIET: DietClient 查询餐食名称
  -> createNutrientTaskAndOutbox(...)
  -> ai_task(PENDING) + 任务 Outbox 同一事务提交
```

营养服务只接受 `RECIPE` 和 `DIET` 目标。目标不存在、没有归属权限或标题为空时，任务不会被创建。

### 5.3 恋爱记录情感任务提交

```text
POST /ai/sentiment/task/love-record-analyze
  -> AiTaskSentimentApplicationServiceImpl
  -> LoveRecordClient 查询 LoveRecordAiSnapshotDTO
  -> 组装 LoveRecordSentimentInputSnapshotDTO
  -> createLoveRecordSentimentTaskAndOutbox(...)
  -> ai_task(PENDING) + 任务 Outbox 同一事务提交
```

快照而不是运行时重新读取恋爱记录，保证重试时使用和首次提交一致的输入，同时避免模型输入随着业务数据修改而漂移。

### 5.4 执行与结果落库

`AiTaskExecutor` 目前按 `taskType` 进行受控分支：

- `RECIPE_NUTRIENT_ANALYSIS`、`DIET_NUTRIENT_ANALYSIS` -> `NutrientTaskProcessor(...)`；
- `LOVE_RECORD_SENTIMENT_ANALYSIS` -> `SentimentTaskProcessor(...)`。

`AiTaskProcessor` 将 `inputSnapshot` 反序列化为对应 DTO，并通过 `AIFactory` 按 `aiType` 选择业务专属的模型实现。虽然同一个模型供应商可以服务营养和情感，但两类业务分别维护 `InvokeNutrientAiService`、`InvokeLoveRecordAIService` 与缓存，避免模型实现把某一种结果格式强行套到另一种任务上。

### 5.5 查询接口

| 用途 | 当前接口 | 返回类型 | 类型保护 |
|---|---|---|---|
| 营养任务状态 | `GET /ai/nutrient/tasks/{taskId}` | `AiTaskStatusVo` | 仅食谱/饮食营养任务 |
| 营养任务结果 | `GET /ai/nutrient/tasks/{taskId}/result` | `AiTaskResultVo` | 仅食谱/饮食营养任务 |
| 情感任务状态 | `GET /ai/sentiment/tasks/{taskId}` | `AiTaskStatusVo` | 仅恋爱记录情感任务 |
| 情感任务结果 | `GET /ai/sentiment/tasks/{taskId}/result` | `AiSentimentTaskResultVo` | 仅恋爱记录情感任务 |
| 任务摘要列表 | `GET /ai/tasks/query/list` | `List<AiTaskSummaryVo>` | 不关联具体结果表 |

结果接口以任务终态决定 HTTP 状态：已终态返回 `200`，尚未终态返回 `202`。所有查询都以当前用户 ID 作为数据归属条件。

---

## 六、数据职责划分

| 表/对象 | 职责 | 不应承担的职责 |
|---|---|---|
| `ai_task` | 任务、状态机、目标快照、输入快照、幂等和执行控制 | 保存所有类型的业务结果字段 |
| `ai_info` | 模型、温度、模型类型、应用类型等调用信息 | 充当业务目标关联中心 |
| `ai_nutrient` | 卡路里、蛋白质、脂肪、碳水、糖、钠等营养结果 | 承载情感、账单或睡眠结果 |
| `ai_sentiment` | 情感标签、分数、摘要、建议 | 承载营养结果 |
| `mq_outbox` | 可靠投递任务事件和终态事件 | 存放最终业务结果 |
| `AiTaskSummaryVo` | 通用任务列表摘要 | 具体营养或情感字段 |
| 专属结果 VO | 某一类结果的展示 | 兼容未来所有完全不同的结果领域 |

---

## 七、代码导航

后续阅读或改造时，优先从下列文件进入：

| 关注点 | 主要位置 | 说明 |
|---|---|---|
| HTTP 入口 | `diary-AI/src/main/java/diary/diaryai/controller/DiaryAIController.java` | 营养、情感的提交/状态/结果接口和通用任务列表。 |
| 营养提交 | `impl/AiTaskNutrientApplicationServiceImpl.java` | 请求规范化、食谱/饮食目标校验、幂等和限流。 |
| 情感提交 | `impl/AiTaskSentimentApplicationServiceImpl.java` | 恋爱记录快照、幂等和限流。 |
| 任务创建与结果事务 | `impl/AiTransactionServiceImpl.java` | 创建 `ai_task + mq_outbox`；营养与情感分别落库。 |
| 执行分派 | `executor/AiTaskExecutor.java` | 按 `taskType` 路由营养/情感。新增任务时的关键修改点。 |
| 快照反序列化与模型选择 | `processor/AiTaskProcessor.java` | 将不同输入快照交给对应 AI 服务。 |
| 模型服务注册 | `factory/AIFactory.java` | 营养与情感两组模型服务缓存、重复编码防护。 |
| 查询与列表保护 | `impl/AiTaskQueryServiceImpl.java` | `taskType` 校验、专属结果查询、分页限制。 |
| SQL 映射 | `resources/mapper/DiaryAIMapper.xml` | 任务、结果、Outbox 与状态迁移 SQL。 |
| 任务语义 | `enums/aienum/AiTaskTypeEnum.java`、`AiFlagEnum.java`、`AiApplicationEnum.java` | 当前任务类型、目标/场景标识和应用编码。 |
| 幂等 | `idempotency/AiRequestFingerprint.java`、`utils/MyUtil.java` | 规范化表单、请求指纹、旧任务快照回填与比对。 |
| 可靠投递/恢复 | `outbox/AiOutboxPublisher.java`、`recovery/job/AiTaskRecoveryJob.java` | Outbox 定时发布和运行超时恢复。 |
| 配置 | `properties/AiTaskProperties.java`、AI 模块配置文件 | Topic、Tag、Consumer Group、重试、租约、限流等。 |

---

## 八、关键问题与对应解决方式

| 问题 | 风险 | 当前解决方式 |
|---|---|---|
| 任务查询强关联 `recipe` | 饮食/情感任务查询不到 | 目标信息固定保存在 `ai_task`，列表不再依赖业务表 `JOIN`。 |
| 所有任务都写 `ai_nutrient` | 情感任务没有落库位置 | 结果按领域拆为 `ai_nutrient`、`ai_sentiment`，事务方法分开。 |
| 前端传 `aiApplication` | 可伪造或混搭业务类型 | 应用编码由营养/情感应用服务内部固定。 |
| `flag` 大小写不一致 | 校验和类型分派可能绕过 | 先 `trim + toUpperCase(Locale.ROOT)` 再校验和使用。 |
| Feign 不携带用户身份 | 下游无法做用户归属校验 | 通过显式 `X-Auth-User-Id` 透传当前用户。 |
| 同一幂等键提交不同数据 | 用户看到错误任务或重复执行 | 数据库唯一约束保证同键唯一；`requestHash` 比对请求内容。 |
| 多 Consumer/旧 Worker 竞争 | 重复落库或状态倒退 | 状态迁移 SQL 校验前置状态、`workerId` 与 `versionId`。 |
| 未完成/失败任务在列表消失 | 用户无法看到任务真实状态 | 通用列表只查 `ai_task`。 |
| 大分页 | 数据库查询压力不可控 | 限制 `pageSize`、页码和偏移量。 |
| 情感记录是隐私数据 | 日志/快照过度泄露 | 输入快照仅保存模型真正需要的数据；禁止日志打印全文；下游必须校验数据归属。 |

---

## 九、新增任务或场景的标准步骤

下面是新增“冲突复盘”“沟通建议”“周期情绪回顾”等任务时的最小检查清单。先判断任务和结果，再开始改代码。

### 9.1 先做四个设计判断

1. **属于已有 AI 应用，还是新应用？**  
   例如冲突复盘、沟通建议都可继续属于“感情分析”；不应因为新增一个任务就机械增加 `AiApplicationEnum`。

2. **复用已有结果结构，还是需要新结果表？**  
   若结果仍是 `sentiment/score/summary/suggestion`，可以复用 `ai_sentiment`；若需要冲突事实、双方需求、沟通步骤等结构化字段，则新增例如 `ai_conflict_reflection`，不要污染 `ai_sentiment`。

3. **目标/场景类型是什么？**  
   按当前语义确定 `targetType`。例如 `LOVE_RECORD_CONFLICT_REFLECTION` 可以明确表达“针对一条恋爱记录的冲突复盘场景”。

4. **模型的稳定输入是什么？**  
   确定最小必要快照。不要在 Consumer 执行时重新读取会变化的业务数据；不要把无关或高度敏感的数据无差别写入快照。

### 9.2 代码和数据改造清单

1. 在 `AiFlagEnum` 增加新的目标/场景标识。
2. 在 `AiTaskTypeEnum` 增加新的 `taskType`，并维护 `getAiTaskType(flag)` 的一对一映射。
3. 仅当它真的是新的产品能力时，才在 `AiApplicationEnum` 增加应用编码。
4. 新增或复用提交 DTO、Controller 接口和应用服务；Controller 不接收由前端决定的 `aiApplication`。
5. 通过受控 Feign Client/服务查询目标对象，完成当前用户归属校验，构造 `targetId`、`targetTitle` 和输入快照。
6. 在 `AiTransactionService` 中以原子事务写入 `ai_task + 任务 Outbox`。任务必须有正确的 `taskType`、目标字段、`requestHash` 和 `inputSnapshot`。
7. 在 `AiTaskExecutor` 增加明确的 `taskType` 分派；需要新输入 DTO 时，在 `AiTaskProcessor` 增加对应的反序列化与调用。
8. 复用或新增专属 `Invoke...AIService`、Prompt 和模型输出校验。若一个模型用于多个业务结果，按业务接口/缓存区分，不要只按供应商类名硬塞逻辑。
9. 复用或新增结果表、PO、Mapper 与结果落库事务；成功路径必须与 `ai_task -> SUCCESS`、完成 Outbox 同一事务提交。
10. 新增专属状态/结果接口、结果 VO 和 `AiTaskQueryServiceImpl` 的 `taskType` 校验；**通用列表通常不需要改**，因为它只返回摘要。
11. 同步 `DiaryAIMapper.xml`、建表 SQL、根目录接口文档和本文。
12. 至少验证：成功、模型返回非法结果、目标不存在/无权限、同幂等键重放、同幂等键不同请求、重复消息、重试耗尽、列表展示和分页边界。

### 9.3 什么时候需要突破当前“一对一 flag -> taskType”映射

当前 `AiTaskTypeEnum.getAiTaskType(flag)` 把一个场景标识映射为一个任务类型。对当前和近期规划而言，这个约束是可接受的：

```text
RECIPE                         -> RECIPE_NUTRIENT_ANALYSIS
DIET                           -> DIET_NUTRIENT_ANALYSIS
LOVE_RECORD                    -> LOVE_RECORD_SENTIMENT_ANALYSIS
LOVE_RECORD_CONFLICT_REFLECTION -> LOVE_RECORD_CONFLICT_REFLECTION
```

只要每种新分析场景都有自己的 `AiFlagEnum` 值和对应 `AiTaskTypeEnum` 值，语义仍然清晰，改动范围也可控。

只有同一个完全相同的 `targetType` 需要由不同入口创建多种可执行任务，而又不希望新增场景标识时，才需要把任务创建改为显式传入 `taskType`，再由任务定义决定应用、目标和处理器。那是未来的重构边界，不是当前必须提前引入的复杂度。

---

## 十、不要破坏的设计约束

- 不要把具体结果字段放回 `AiTaskSummaryVo`；列表只返回公共任务摘要。
- 不要在通用列表中 `INNER JOIN ai_nutrient`、`ai_sentiment` 或业务表。
- 不要让前端传入并决定 `aiApplication`、`taskType` 的任意组合。
- 不要仅凭目标 ID 信任跨服务数据；每次读取目标对象都必须携带并校验当前用户身份。
- 不要在任务执行时重新拼装可变的业务输入；使用提交时 `inputSnapshot`。
- 不要在成功事务外单独写结果、状态或完成事件；否则会出现“有结果无成功状态”等不一致。
- 不要去掉 `workerId + versionId` 的状态迁移条件；它们是抵抗重复消费和过期 Worker 的保护。
- 不要把恋爱记录全文、模型原始响应或令牌等敏感内容写入普通日志。
- 不要认为“配置文件里有一个配置项”就代表功能已生效；每次改 Topic、Tag、Consumer Group、定时周期、限流或 TTL 时，都要确认 `@ConfigurationProperties` 已绑定并且实际代码引用了该属性。

---

## 十一、部署与安全边界

`CurrentUser` 读取 `X-Auth-User-Id`。网关清除客户端伪造的同名头并重新写入时，这条链路可以使用；但如果客户端能直接访问 `diary-ai`、`diary-love` 等内部服务端口，就可能伪造该请求头，形成横向越权。

生产环境应至少满足：

- AI、Love、Recipe、Diet 等模块端口只对网关或可信内部网络开放；
- 网关是外部访问的唯一入口，并负责身份校验和身份头重写；
- 服务间调用的身份透传保留用户归属校验；
- 若未来存在绕过网关的服务调用，应增加可信服务身份校验，而不是只依赖用户头。

---

## 十二、当前仍需保持警惕的点

以下不是要求立即大重构，而是后续改动时必须重新核对的边界：

- `AiTaskExecutor` 和查询服务当前存在显式任务类型分支。新增任务时必须同步添加执行分派和查询保护，避免“创建成功但无法执行”或“结果接口类型不匹配”。
- `AiTaskTypeEnum.getAiTaskType(flag)` 是当前任务创建的中心映射。新增 `AiFlagEnum` 而不更新这里，会导致创建失败或映射到错误任务类型。
- 结果查询的 API 路径和 Controller 返回的 `Location` 头要保持一致；新增专属接口时不要只复制任务创建路径。
- AI 模型输出解析必须把异常归类为可重试还是永久失败。输出 JSON 格式不合法、字段不合法一般应作为永久错误，避免无意义重复调用。
- 情感任务的快照和模型提示词是隐私边界。后续增加“冲突复盘”时，优先增加结构化、最小化输入，而不是整段无筛选文本。

---

## 十三、变更记录

| 日期 | 变更 | 影响/结论 |
|---|---|---|
| 2026-10-23 | 创建本文，复盘从单一食谱营养分析到营养 + 恋爱记录情感分析的渐进式拆分。 | 明确任务中心、结果分表、摘要列表、专属详情和后续扩展导航。 |

新增任务后请在此追加记录，建议格式为：

```text
| YYYY-MM-DD | 新增 TASK_TYPE / 场景名称；涉及入口、快照、结果表、MQ 或查询变化。 | 说明复用/新增了什么，以及验证结果。 |
```

---

## 结语

本次演进的核心不是建立一套“万能 AI 框架”，而是把原先绑定在食谱营养场景上的职责拆开：

```text
ai_task 负责稳定的任务生命周期和目标快照
结果表    负责各领域自己的结构化结果
专属接口  负责各任务的提交、查询与类型保护
摘要列表  负责统一展示，不假装理解所有结果
```

因此，后续新增任务不需要重写 `diary-AI`：沿着目标快照、任务类型、执行分派、结果落库和专属查询这条链路补齐即可。只要不重新把业务目标、结果结构和通用查询绑回同一条 SQL，模块就能继续以可控的代码量增长。
