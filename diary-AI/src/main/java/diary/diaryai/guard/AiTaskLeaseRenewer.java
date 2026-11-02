package diary.diaryai.guard;

import diary.diaryai.entity.ai.dto.AiTaskProcessDto;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
/**
 * 在模型调用期间续期任务租约，防止耗时任务被其他实例误接管。
 *
 * <p>续租动作由独立的单线程调度器执行，并通过 Micrometer 暴露标准的 executor 指标。
 * 可用 {@code executor_active_threads}、{@code executor_queued_tasks}、
 * {@code executor_completed_tasks_total}，配合 {@code name=diary.ai.lease-renewer}
 * 标签观察续租线程是否阻塞、积压或停止工作。</p>
 */
public class AiTaskLeaseRenewer {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskProperties properties;
    private final ScheduledExecutorService scheduler;

    public AiTaskLeaseRenewer(DiaryAiMapper diaryAiMapper, AiTaskProperties properties, MeterRegistry meterRegistry) {
        this.diaryAiMapper = diaryAiMapper;
        this.properties = properties;
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, new LeaseThreadFactory());
        // LeaseRenewalHandle.close() 取消任务后立即移出队列，避免已完成任务长期占用调度器队列。
        executor.setRemoveOnCancelPolicy(true);

        // 注册标准线程池指标；固定 name 标签便于筛选，且不会产生 taskId/userId 等高基数时序。
        this.scheduler = ExecutorServiceMetrics.monitor(
                meterRegistry,
                executor,
                "diary.ai.lease-renewer"
        );
    }

    public LeaseRenewalHandle start(Long taskId, String workerId, Integer versionId) {
        long leaseSeconds = properties.getTask().getExecutionLeaseSeconds();
        long renewalIntervalSeconds = Math.max(1L, leaseSeconds / 3L);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                AiTaskProcessDto renewal = AiTaskProcessDto.builder()
                        .taskId(taskId)
                        .workerId(workerId)
                        .versionId(versionId)
                        .leaseSeconds(leaseSeconds)
                        .build();
                int renewed = diaryAiMapper.renewExecutionLease(renewal);
                if (renewed != 1) {
                    log.debug("停止续租前未找到当前Worker所有权, taskId={}, workerId={}", taskId, workerId);
                }
            } catch (RuntimeException e) {
                // 单次续租失败不能中断正在执行的模型调用；后续周期继续尝试，最终仍由 version CAS 保护结果提交。
                log.warn("AI任务租约续期失败, taskId={}, workerId={}", taskId, workerId, e);
            }
            // scheduler.scheduleAtFixedRate(..., time1, time2, timeunit)
            // 表示延迟time1后，以time2恒定运行
        }, renewalIntervalSeconds, renewalIntervalSeconds, TimeUnit.SECONDS);
        return () -> future.cancel(false);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }

    @FunctionalInterface
    public interface LeaseRenewalHandle extends AutoCloseable {
        @Override
        void close();
    }

    static final class LeaseThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "ai-task-lease-renewer");
            thread.setDaemon(true);
            return thread;
        }
    }
}
