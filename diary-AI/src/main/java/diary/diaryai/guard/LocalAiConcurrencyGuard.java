package diary.diaryai.guard;

import diary.diaryai.properties.AiTaskProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
/**
 * 单实例内的模型调用并发保护。
 *
 * <p>Semaphore 只限制当前 JVM，不等同于跨实例的全局限流。指标含义：
 * {@code diary_ai_concurrency_available} 是剩余许可，
 * {@code diary_ai_concurrency_max} 是配置上限，
 * {@code diary_ai_concurrency_rejected_total} 是并发已满而拒绝处理的累计次数。</p>
 */
public class LocalAiConcurrencyGuard {
    private final Semaphore semaphore;
    private final long waitMs;
    private final Counter rejectedCounter;
    private final Counter interruptedCounter;

    public LocalAiConcurrencyGuard(AiTaskProperties properties, MeterRegistry meterRegistry) {
        int maxConcurrency = properties.getLimit().getModelLocalConcurrency();
        this.semaphore = new Semaphore(maxConcurrency, true);
        this.waitMs = properties.getLimit().getLocalPermitWaitMs();
        // 只使用固定标签/无动态标签，避免 taskId、userId 等导致 Prometheus 时序数量膨胀。
        this.rejectedCounter = Counter.builder("diary.ai.concurrency.rejected")
                .description("AI tasks rejected because the local concurrency limit was reached")
                .register(meterRegistry);
        this.interruptedCounter = Counter.builder("diary.ai.concurrency.interrupted")
                .description("AI concurrency permit waits interrupted before acquisition")
                .register(meterRegistry);
        Gauge.builder("diary.ai.concurrency.available", semaphore, Semaphore::availablePermits)
                .description("Available local AI concurrency permits")
                .register(meterRegistry);
        Gauge.builder("diary.ai.concurrency.max", () -> maxConcurrency)
                .description("Configured local AI concurrency limit")
                .register(meterRegistry);
    }

    public boolean tryAcquire() {
        try {
            boolean acquired = semaphore.tryAcquire(waitMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                // 消费者会返回 FAILURE 触发 RocketMQ 重投；该计数用于判断本机并发上限是否偏低。
                rejectedCounter.increment();
            }
            return acquired;
        } catch (InterruptedException e) {
            interruptedCounter.increment();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void release() {
        semaphore.release();
    }
}
