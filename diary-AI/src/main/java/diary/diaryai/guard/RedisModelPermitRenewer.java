package diary.diaryai.guard;

import diary.diaryai.enums.aienum.AIEnum;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.redis.RedisModelPermitManager;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * 职责边界：定时触发 renew，并维护 permit 是否已丢失
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisModelPermitRenewer {
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(new RedisModelPermitRenewer.PermitRenewerThreadFactory());
    private final RedisModelPermitManager redisModelPermitManager;
    private final AiTaskProperties properties;

    public PermitRenewalHandle start(AIEnum model, String permitId) {
        long renewalIntervalSeconds = properties.getLimit().getGlobalModelPermit().getRenewalIntervalSeconds();
        AtomicBoolean permitLost = new AtomicBoolean(false);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            if (permitLost.get()) {
                return;
            }
            try {
                if (!redisModelPermitManager.renew(model, permitId)) {
                    permitLost.set(true);
                    log.warn("Redis permit 已丢失, model={}, permitId={}", model, permitId);
                }
            } catch (RuntimeException redisException) {
                log.warn("Redis permit 续期异常, model={}", model, redisException);
            }
        }, renewalIntervalSeconds, renewalIntervalSeconds, TimeUnit.SECONDS);

        return new PermitRenewalHandle() {
            @Override
            public boolean isPermitLost() {
                return permitLost.get();
            }

            @Override
            public void close() {
                future.cancel(false);
            }
        };
    }

    public interface PermitRenewalHandle extends AutoCloseable {

        boolean isPermitLost();

        @Override
        void close();
    }

    static final class PermitRenewerThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "redis-model-permit-renewer");
            thread.setDaemon(true);
            return thread;
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }
}
