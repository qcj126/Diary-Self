package diary.diaryai.redis;

import diary.common.exception.ParamIllegalException;
import diary.diaryai.enums.aienum.AIEnum;
import diary.diaryai.properties.AiTaskProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/*
 * 职责边界：针对指定 AIEnum 与 permitId，返回“是否拿到 permit”。
 * */
@Component
@RequiredArgsConstructor
public class RedisModelPermitManager {
    private final StringRedisTemplate stringRedisTemplate;
    private final AiTaskProperties aiTaskProperties;
    private final AiRedisKeyFactory aiRedisKeyFactory;

    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT = new DefaultRedisScript<>(
            """
                          local time = redis.call('TIME')
                          local nowMillis = time[1] * 1000 + math.floor(time[2] / 1000)
                    
                          redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', nowMillis)
                    
                          local permitId = ARGV[3]
                          local leaseMillis = tonumber(ARGV[2])
                          local expiresAt = nowMillis + leaseMillis
                    
                          if redis.call('ZSCORE', KEYS[1], permitId) then
                              redis.call('ZADD', KEYS[1], expiresAt, permitId)
                              return 1
                          end
                    
                          local maxConcurrency = tonumber(ARGV[1])
                          if redis.call('ZCARD', KEYS[1]) >= maxConcurrency then
                              return 0
                          end
                    
                          redis.call('ZADD', KEYS[1], expiresAt, permitId)
                          return 1
                    """, Long.class);
    private static final DefaultRedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>(
            """
                        local time = redis.call('TIME')
                        local nowMillis = time[1] * 1000 + math.floor(time[2] / 1000)
                    
                        redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', nowMillis)
                    
                        local permitId = ARGV[2]
                        if not redis.call('ZSCORE', KEYS[1], permitId) then
                            return 0
                        end
                    
                        local leaseMillis = tonumber(ARGV[1])
                        redis.call('ZADD', KEYS[1], nowMillis + leaseMillis, permitId)
                        return 1
                    """, Long.class);

    // 返回false时，只代表模型并发已达上限，获取perimit资源
    public boolean tryAcquire(AIEnum model, String permitId) {
        Objects.requireNonNull(model, "model must not be null");
        if (permitId == null || permitId.isBlank()) throw new ParamIllegalException("permitId不能为空");
        // 读取被调用模型的最大并发量
        AiTaskProperties.ModelPermit modelPermit = aiTaskProperties.getLimit().getGlobalModelPermit().getModels().get(model);
        if (modelPermit == null) throw new IllegalStateException("未配置模型全局并发配额: " + model);
        int maxConcurrency = modelPermit.getMaxConcurrency();
        // 从 leaseSeconds 计算 leaseMillis
        int leaseSeconds = aiTaskProperties.getLimit().getGlobalModelPermit().getLeaseSeconds();
        long leaseMillis = Duration.ofSeconds(leaseSeconds).toMillis();

        // 执行lua脚本
        Long result = stringRedisTemplate.execute(
                ACQUIRE_SCRIPT,
                List.of(aiRedisKeyFactory.globalModelPermit(model)),
                String.valueOf(maxConcurrency),
                String.valueOf(leaseMillis),
                permitId
        );

        return Long.valueOf(1L).equals(result);
    }

    // 释放permit资源
    public boolean release(AIEnum model, String permitId) {
        Objects.requireNonNull(model, "model must not be null");
        if (permitId == null || permitId.isBlank()) throw new ParamIllegalException("permitId不能为空");
        Long removed = stringRedisTemplate.opsForZSet().remove(aiRedisKeyFactory.globalModelPermit(model), permitId);
        return removed != null && removed > 0;
    }

    // 为长任务的permit进行续期
    public boolean renew(AIEnum model, String permitId) {
        Objects.requireNonNull(model, "model must not be null");
        if (permitId == null || permitId.isBlank()) throw new ParamIllegalException("permitId不能为空");
        // 从 leaseSeconds 计算 leaseMillis
        int leaseSeconds = aiTaskProperties.getLimit().getGlobalModelPermit().getLeaseSeconds();
        long leaseMillis = Duration.ofSeconds(leaseSeconds).toMillis();

        Long result = stringRedisTemplate.execute(
                RENEW_SCRIPT,
                List.of(aiRedisKeyFactory.globalModelPermit(model)),
                String.valueOf(leaseMillis),
                permitId
        );

        return Long.valueOf(1L).equals(result);
    }
}
