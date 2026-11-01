# Redis 全局模型并发控制：Docker 与 Linux 运维验证手册

## 1. 适用范围与安全原则

本文用于开发、测试或隔离预发布环境的部署和验证。

- 不要在共享环境或生产环境直接停止 Redis 做故障演练；
- 不要把数据库、Redis、模型供应商或 Nacos 的密码写入命令历史、文档或日志；
- 密码请通过交互输入、环境变量注入、Nacos 加密配置或密钥管理服务提供；
- 执行迁移前先备份或确认数据库变更流程。

以下 `<...>` 表示需要替换的环境名称，不是可直接复制的真实值。

## 2. 部署前检查

```bash
docker compose ps
docker compose logs --tail=100 <diary-ai-service>
```

Linux 上使用 systemd 部署时：

```bash
systemctl status <diary-ai-service>
journalctl -u <diary-ai-service> -n 200 --no-pager
```

确认：

- MySQL、Redis、RocketMQ、Nacos（如使用）均可用；
- `diary-ai` 启动时没有 `global-model-permit` 配置校验异常；
- 每个 `AIEnum` 都存在模型并发配置；
- `renewal-interval-seconds` 小于 `lease-seconds`。

## 3. 执行 Outbox 因果去重迁移

迁移文件：

```text
diary-common/src/main/resources/sql/ai_task_outbox_retry_source_event_migration.sql
```

迁移增加：

```text
mq_outbox.source_event_id
UNIQUE(event_type, source_event_id)
```

### 3.1 Linux / 本机 MySQL

以下命令会提示交互输入密码，不要在命令行中拼接密码：

```bash
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p "$MYSQL_DATABASE" \
  < diary-common/src/main/resources/sql/ai_task_outbox_retry_source_event_migration.sql
```

### 3.2 Docker 中的 MySQL

先复制脚本，再进入 MySQL 容器交互执行：

```bash
docker cp diary-common/src/main/resources/sql/ai_task_outbox_retry_source_event_migration.sql \
  <mysql-container>:/tmp/ai_task_outbox_retry_source_event_migration.sql

docker exec -it <mysql-container> sh
mysql -u <mysql-user> -p <database-name> \
  < /tmp/ai_task_outbox_retry_source_event_migration.sql
exit
```

### 3.3 验证迁移

```sql
SHOW COLUMNS FROM mq_outbox LIKE 'source_event_id';
SHOW INDEX FROM mq_outbox WHERE Key_name = 'uk_retry_source_event';
```

迁移只应执行一次。若环境已经执行过，请先查询表结构，不要重复执行 `ADD COLUMN`。

## 4. 构建与启动

Docker Compose：

```bash
docker compose up -d --build <diary-ai-service>
docker compose logs --tail=200 -f <diary-ai-service>
```

单个容器镜像：

```bash
docker build -t diary-ai:<tag> .
docker run --rm --name <diary-ai-container> diary-ai:<tag>
```

使用 systemd 时，发布新包后：

```bash
sudo systemctl restart <diary-ai-service>
sudo journalctl -u <diary-ai-service> -f
```

## 5. Redis permit 观察命令

请把前缀和模型名替换为当前环境配置。建议使用交互密码模式：

```bash
redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" --askpass \
  ZCARD 'diary:dev:ai:global-permit:model:QWEN37PLUS'

redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" --askpass \
  ZRANGE 'diary:dev:ai:global-permit:model:QWEN37PLUS' 0 -1 WITHSCORES
```

执行模型期间，`ZCARD` 应不超过 YAML 中该模型的 `max-concurrency`；正常结束后应回到 `0`。若 Worker 崩溃，permit 会在 lease 到期后由后续获取操作清理。

## 6. MySQL 观察 SQL

### 6.1 查看任务状态

```sql
SELECT id, status, attempt_count, worker_id, lease_until,
       error_code, error_message, version_id, update_time
FROM ai_task
WHERE id = <task-id>;
```

### 6.2 查看任务对应的 Outbox

```sql
SELECT id, event_id, source_event_id, event_type, status,
       retry_count, next_retry_time, broker_message_id,
       create_time, sent_time, update_time
FROM mq_outbox
WHERE aggregate_id = <task-id>
ORDER BY create_time, id;
```

### 6.3 查找 Retry 分叉

```sql
SELECT source_event_id, COUNT(*) AS retry_count
FROM mq_outbox
WHERE event_type = 'AI_TASK_RETRY'
  AND source_event_id IS NOT NULL
GROUP BY source_event_id
HAVING COUNT(*) > 1;
```

预期为空。

## 7. 验证场景

### 7.1 正常单任务

1. 使用现有已登录界面或安全的调试工具提交一个任务；不要在日志或文档中粘贴认证 token。
2. 等待任务完成。
3. 任务应为 `SUCCESS`，Redis 对应模型 ZSet 不应保留 permit。
4. Outbox 通常包含 `AI_TASK_CREATED` 与 `AI_COMPLETED`。

### 7.2 模型池容量满

仅在开发环境，将待测模型临时设为：

```yaml
max-concurrency: 1
```

重启服务后，近似同时提交两个同模型、不同 `clientRequestId` 的任务。

预期第二任务：

```text
status        = RETRY_WAIT
error_code    = MODEL_CAPACITY_EXHAUSTED
attempt_count 不增加
worker_id     为空
lease_until   为空
```

并生成未来约 10～13 秒可发送的 `AI_TASK_RETRY`。测试结束后恢复正常并发配置并重启服务。

### 7.3 Redis 不可用

提交 API 也使用 Redis 限流器，因此不能先停止 Redis 再提交任务。推荐在隔离开发环境按以下步骤演练：

```text
Redis 正常时提交任务
→ 立即暂停 diary-ai 实例，确保任务尚未消费
→ 验证 ai_task 为 PENDING / QUEUED
→ 暂停 Redis
→ 恢复 diary-ai 实例
```

Docker 示例：

```bash
docker pause <diary-ai-container>
docker pause <redis-container>
docker unpause <diary-ai-container>
```

此时预期：

```text
task.status     = RETRY_WAIT
task.error_code = REDIS_UNAVAILABLE
模型执行器不应被调用
AI_TASK_RETRY.next_retry_time ≈ 当前时间 + 30～33 秒
```

恢复 Redis：

```bash
docker unpause <redis-container>
```

等待下一轮 Retry Outbox 到期并被 Publisher 扫描。Publisher 扫描周期会额外引入最多一个扫描周期的等待。任务最终应进入：

```text
RETRY_WAIT → QUEUED → RUNNING → SUCCESS
```

### 7.4 Docker 日志定位

```bash
docker compose logs --since 10m <diary-ai-service> \
  | grep -E 'Redis 模型 permit|延迟重试|AI_TASK_RETRY|taskId='
```

PowerShell：

```powershell
docker compose logs --since 10m <diary-ai-service> |
  Select-String 'Redis 模型 permit|延迟重试|AI_TASK_RETRY|taskId='
```

## 8. 常见排障

| 现象 | 重点检查 |
| --- | --- |
| 任务停在 `QUEUED` | 是否存在已 `SENT` 的 Retry Outbox；查看 Consumer 是否处理了提前到达的 `AI_TASK_RETRY` |
| Redis ZSet 长期有 member | Worker 是否仍在执行；续期日志；permit lease 是否已到期；release 是否异常 |
| Redis 不可用但任务进入 `RUNNING` | 检查 Consumer 是否绕过 `RedisModelPermitManager.tryAcquire`；这是 fail-closed 违规 |
| 同一来源出现多条 Retry | 检查 `source_event_id` 迁移与唯一索引是否存在；执行 Retry 分叉 SQL |
| 应用 SQL 报未知列 `source_event_id` | 数据库迁移尚未执行，或应用版本先于迁移发布 |
| Redis 恢复后仍未执行 | 检查 `AI_TASK_RETRY.next_retry_time`、Outbox 状态、Publisher 扫描日志与 RocketMQ Consumer Group 堆积 |

## 9. 收尾检查清单

- [ ] 正常模型限额已恢复；
- [ ] 迁移已在当前环境执行；
- [ ] 单任务、容量满、Redis 不可用场景已验证；
- [ ] `source_event_id` Retry 分叉查询为空；
- [ ] 没有遗留 Redis permit；
- [ ] 不在日志、脚本或文档中保存密码和 token。
