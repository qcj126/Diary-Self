package diary.diaryai.redis;

import diary.diaryai.properties.AiTaskProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AiSubmitRateLimiter {
    private static final DefaultRedisScript<Long> RATE_SCRIPT =
            new DefaultRedisScript<>("""
                local current = redis.call('INCR', KEYS[1])
                if current == 1 then
                    redis.call('EXPIRE', KEYS[1], ARGV[1])
                end
                return current
                """, Long.class);
    private final StringRedisTemplate redisTemplate;
    private final AiRedisKeyFactory keyFactory;
    private final AiTaskProperties properties;

    public boolean allow(Long userId) {
        long epochMinute = Instant.now().getEpochSecond() / 60;
        String key = keyFactory.submitRate(userId, epochMinute);
        Long current = redisTemplate.execute(
                RATE_SCRIPT,
                List.of(key),
                "120");
        return current <= properties.getLimit().getSubmitPerUserPerMinute();
    }
}
