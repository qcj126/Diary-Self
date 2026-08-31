package diary.diaryai.guard;

import diary.common.entity.ai.dto.AiTaskProcessDto;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AiTaskLeaseRenewer {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskProperties properties;
    private final ScheduledExecutorService scheduler;

    public AiTaskLeaseRenewer(DiaryAiMapper diaryAiMapper, AiTaskProperties properties) {
        this.diaryAiMapper = diaryAiMapper;
        this.properties = properties;
        int schedulerThreads = Math.max(1, properties.getLimit().getModelLocalConcurrency());
        this.scheduler = Executors.newScheduledThreadPool(schedulerThreads, new LeaseThreadFactory());
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

    private static final class LeaseThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "ai-task-lease-renewer");
            thread.setDaemon(true);
            return thread;
        }
    }
}
