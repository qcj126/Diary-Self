package diary.diaryai.impl;

import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.redis.AiIdempotencyCacheService;
import diary.diaryai.redis.AiRedisKeyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiIdempotencyCacheServiceImpl implements AiIdempotencyCacheService {
    private final StringRedisTemplate redisTemplate;
    private final AiRedisKeyFactory keyFactory;
    private final AiTaskProperties properties;

    public Optional<Long> get(Long userId, String clientRequestId) {
        try {
            String value = redisTemplate.opsForValue().get(
                    keyFactory.idempotency(userId, clientRequestId));
            return value == null ? Optional.empty() : Optional.of(Long.valueOf(value));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public void put(Long userId, String clientRequestId, Long taskId) {
        try {
            redisTemplate.opsForValue().set(
                    keyFactory.idempotency(userId, clientRequestId),
                    taskId.toString(),
                    Duration.ofHours(properties.getCache().getIdempotencyTtlHours()));
        } catch (RuntimeException e) {
            log.warn("写入AI幂等缓存失败, taskId={}", taskId, e);
        }
    }

    public void evict(Long userId, String clientRequestId) {
        redisTemplate.delete(keyFactory.idempotency(userId, clientRequestId));
    }
}
