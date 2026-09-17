# XXL-Job 执行器任务配置

## 天气缓存刷新

执行器已注册 `weatherRefreshHandler`。请在 XXL-Job 管理端创建并启动任务：

- JobHandler：`weatherRefreshHandler`
- 调度类型：`CRON`
- Cron：`0 0 0/6 * * ?`
- 阻塞处理策略：单机串行
- 路由策略：第一个

该表达式会在每天 `00:00`、`06:00`、`12:00`、`18:00` 刷新天气缓存。缓存 key 沿用 `weather:{城市名}`，默认 TTL 为 7 小时，刷新失败不会覆盖已有缓存。

和风天气凭据必须通过 Nacos 或环境变量配置：

```yaml
weather:
  qweather:
    api-host: ${QWEATHER_API_HOST}
    api-key: ${QWEATHER_API_KEY}
```

## 未关联图片清理

执行器已注册 `spareImageCleanUpHandler`。任务会进行两类清理：

1. 删除超过保护期、且未被 `time_card`、`recipe`、`love_record_image` 关联的 `image` 记录及 OSS 对象。
2. 删除受控图片前缀下、超过保护期且在 `image` 表中不存在的 OSS 对象。

默认保护期为 24 小时，单次每类最多删除 200 个对象。可通过 Nacos 覆盖：

```yaml
image:
  cleanup:
    retention-hours: 24
    batch-size: 200
    oss-prefixes: food_path_,cook_path_,ingredient_path_,bean_matching_path_,gift_path_,snack_path_,tea_path_,travel_path_,daily_path_,exercise_path_,walk_path
```

建议先在测试环境将 `batch-size` 调小并人工触发 `spareImageCleanUpHandler`，确认业务表和 OSS 前缀与实际环境一致后再启用周期调度。
