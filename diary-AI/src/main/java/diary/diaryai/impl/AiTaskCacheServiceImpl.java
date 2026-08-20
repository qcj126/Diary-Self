package diary.diaryai.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.ai.vo.AiTaskStatusVo;
import diary.common.enums.aienum.AiTaskStatusEnum;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.redis.AiRedisKeyFactory;
import diary.diaryai.redis.AiTaskCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiTaskCacheServiceImpl implements AiTaskCacheService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AiRedisKeyFactory keyFactory;
    private final AiTaskProperties properties;

    public Optional<AiTaskStatusVo> get(Long taskId) {
        try {
            String json = redisTemplate.opsForValue().get(keyFactory.task(taskId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, AiTaskStatusVo.class));
        } catch (Exception e) {
            log.warn("读取AI任务缓存失败, taskId={}", taskId, e);
            return Optional.empty();
        }
    }

    public void put(AiTaskStatusVo value) {
        try {
            boolean terminal = AiTaskStatusEnum.valueOf(value.getStatus()).isTerminal();
            Duration ttl = terminal
                    ? Duration.ofHours(properties.getCache().getTerminalTtlHours())
                    : Duration.ofSeconds(properties.getCache().getRunningTtlSeconds());
            long jitter = ThreadLocalRandom.current().nextLong(0, 5);
            redisTemplate.opsForValue().set(
                    keyFactory.task(value.getTaskId()),
                    objectMapper.writeValueAsString(value),
                    ttl.plusSeconds(jitter));
        } catch (Exception e) {
            log.warn("写入AI任务缓存失败, taskId={}", value.getTaskId(), e);
            throw new RuntimeException("缓存写入失败，请稍后重试", e);
        }
    }

    public void evict(Long taskId) {
        try {
            redisTemplate.delete(keyFactory.task(taskId));
        } catch (RuntimeException e) {
            log.warn("删除AI任务缓存失败, taskId={}", taskId, e);
            throw new RuntimeException("缓存清理失败，请稍后重试", e);
        }
    }
}
