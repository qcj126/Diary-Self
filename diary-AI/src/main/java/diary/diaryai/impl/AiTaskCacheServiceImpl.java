package diary.diaryai.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.ai.vo.AiTaskStatusVo;
import diary.common.enums.aienum.AiTaskStatusEnum;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.redis.AiRedisKeyFactory;
import diary.diaryai.redis.AiTaskCacheService;
import lombok.RequiredArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    @Override
    public Optional<AiTaskStatusVo> get(Long taskId, Long userId) {
        try {
            String json = redisTemplate.opsForValue().get(keyFactory.task(taskId));
            if (json == null) {
                return Optional.empty();
            }
            TaskCacheEntry entry = objectMapper.readValue(json, TaskCacheEntry.class);
            /*
             * 改前：状态缓存只按 taskId 命中，新增数据库归属校验后仍可能从缓存绕过 userId 校验。
             * 改后：缓存条目保存 owner userId，所有者不匹配时按未命中处理并回查带 userId 条件的数据库。
             */
            return userId.equals(entry.getUserId()) ? Optional.ofNullable(entry.getValue()) : Optional.empty();
        } catch (Exception e) {
            log.warn("读取AI任务缓存失败, taskId={}", taskId, e);
            return Optional.empty();
        }
    }

    @Override
    public void put(AiTaskStatusVo value, Long userId) {
        try {
            boolean terminal = AiTaskStatusEnum.valueOf(value.getStatus()).isTerminal();
            Duration ttl = terminal
                    ? Duration.ofHours(properties.getCache().getTerminalTtlHours())
                    : Duration.ofSeconds(properties.getCache().getRunningTtlSeconds());
            long jitter = ThreadLocalRandom.current().nextLong(0, 5);
            redisTemplate.opsForValue().set(
                    keyFactory.task(value.getTaskId()),
                    objectMapper.writeValueAsString(new TaskCacheEntry(userId, value)),
                    ttl.plusSeconds(jitter));
        } catch (Exception e) {
            log.warn("写入AI任务缓存失败, taskId={}", value.getTaskId(), e);
            /*
             * 改前：task + outbox 已经提交后，Redis 写失败仍向 Controller 抛异常，客户端得到 500，
             * 但任务实际上已经受理，形成典型的“结果不明确”。
             * 改后：缓存作为旁路优化 fail-open，只记录告警，权威状态始终由 MySQL 提供。
             */
        }
    }

    @Override
    public void evict(Long taskId) {
        try {
            redisTemplate.delete(keyFactory.task(taskId));
        } catch (RuntimeException e) {
            log.warn("删除AI任务缓存失败, taskId={}", taskId, e);
            // 缓存清理失败不应回滚或改变已经提交的任务状态，TTL 会提供最终兜底。
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TaskCacheEntry {
        private Long userId;
        private AiTaskStatusVo value;
    }
}
