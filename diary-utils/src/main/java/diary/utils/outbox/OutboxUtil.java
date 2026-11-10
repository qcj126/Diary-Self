package diary.utils.outbox;

import java.util.concurrent.ThreadLocalRandom;

public class OutboxUtil {
    public static long calculateRetryDelaySeconds(Integer retryCount, int maxRetryCount) {
        long baseSeconds = Math.min(1L << Math.min(retryCount, maxRetryCount), 120L) * 5L;
        long jitterSeconds = ThreadLocalRandom.current().nextLong(0, 4);
        return Math.min(baseSeconds, 600L) + jitterSeconds;
    }
}
