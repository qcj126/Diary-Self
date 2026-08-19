package diary.diaryai.guard;

import diary.diaryai.properties.AiTaskProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
    public class LocalAiConcurrencyGuard {
        private final Semaphore semaphore;
        private final long waitMs;

        public LocalAiConcurrencyGuard(AiTaskProperties properties) {
            this.semaphore = new Semaphore(properties.getLimit().getModelLocalConcurrency(), true);
            this.waitMs = properties.getLimit().getLocalPermitWaitMs();
        }

        public boolean tryAcquire() {
            try {
                return semaphore.tryAcquire(waitMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        public void release() {
            semaphore.release();
        }
    }
