package diary.diaryai.metrics;

import diary.diaryai.enums.aienum.AiTaskStatusEnum;
import diary.diaryai.enums.outbox.OutboxStatusEnum;
import diary.diaryai.mapper.DiaryAiMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 将 AI 任务与 Outbox 的状态数量暴露为 Prometheus Gauge。
 *
 * <p>不在 Prometheus 每次抓取时查询数据库：定时任务把聚合结果缓存到内存，抓取端点只读取
 * AtomicLong。status 标签只取固定枚举值，避免业务 ID 造成高基数时序。</p>
 */
@Slf4j
@Component
public class AiOperationalMetrics {
    private final DiaryAiMapper diaryAiMapper;
    /**
     * 任务状态对应的内存快照。Gauge 始终绑定这些 AtomicLong，更新时无需重复注册 Meter。
     */
    private final Map<String, AtomicLong> taskStatusCounts;
    /**
     * Outbox 状态对应的内存快照。与任务状态分开保存，避免两类状态同名时混淆语义。
     */
    private final Map<String, AtomicLong> outboxStatusCounts;

    public AiOperationalMetrics(DiaryAiMapper diaryAiMapper, MeterRegistry meterRegistry) {
        this.diaryAiMapper = diaryAiMapper;
        this.taskStatusCounts = registerStatusGauges(
                meterRegistry,
                "diary.ai.task.status.count",
                "Number of AI tasks grouped by status",
                Arrays.stream(AiTaskStatusEnum.values()).map(Enum::name).toList()
        );
        this.outboxStatusCounts = registerStatusGauges(
                meterRegistry,
                "diary.ai.outbox.status.count",
                "Number of AI outbox messages grouped by status",
                Arrays.stream(OutboxStatusEnum.values()).map(Enum::name).toList()
        );
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSnapshot() {
        // 应用就绪后先刷新一次，避免首个 Prometheus 抓取周期读到初始的全 0 数据。
        refreshSnapshot();
    }

    /**
     * 定期从数据库刷新状态数量。
     *
     * <p>Prometheus 可能同时抓取多个实例；若在 HTTP 抓取端点中直接查库，会把监控流量放大为数据库压力，
     * 因此此处采用“低频查库 + 高频读内存 Gauge”的方式。</p>
     */
    @Scheduled(fixedDelayString = "${diary.ai.metrics.snapshot-interval-ms:15000}")
    public void refreshSnapshot() {
        try {
            updateCounts(taskStatusCounts, diaryAiMapper.selectAiTaskStatusCounts());
            updateCounts(outboxStatusCounts, diaryAiMapper.selectOutboxStatusCounts());
        } catch (RuntimeException exception) {
            // 保留上一次成功快照，避免数据库短暂故障时将所有监控值误报为 0。
            log.warn("刷新 AI 运营指标快照失败", exception);
        }
    }

    private Map<String, AtomicLong> registerStatusGauges(
            MeterRegistry meterRegistry, String metricName, String description, Iterable<String> statuses) {
        Map<String, AtomicLong> counters = new LinkedHashMap<>();
        for (String status : statuses) {
            AtomicLong count = new AtomicLong();
            counters.put(status, count);
            Gauge.builder(metricName, count, AtomicLong::get)
                    .description(description)
                    .tag("status", status)
                    .register(meterRegistry);
        }
        return counters;
    }

    /**
     * 用本次完整聚合结果覆盖旧快照；数据库未返回的既有状态归零，例如最后一条 DEAD 消息已被人工处理时。
     */
    private void updateCounts(Map<String, AtomicLong> counters, Iterable<StatusCount> rows) {
        counters.values().forEach(count -> count.set(0));
        for (StatusCount row : rows) {
            AtomicLong count = counters.get(row.getStatus());
            if (count != null) {
                count.set(row.getTotal());
            } else {
                log.warn("收到未注册的 AI 状态监控值, status={}", row.getStatus());
            }
        }
    }
}
