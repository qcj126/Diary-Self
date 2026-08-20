package diary.diaryinfo.impl;

import diary.diaryinfo.service.SysInfoCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SysInfoCacheServiceImpl implements SysInfoCacheService {
    private final RedisTemplate<String, Object> diaryRedisTemplate;

    public SysInfoCacheServiceImpl(
            @Qualifier("diaryRedisTemplate") RedisTemplate<String, Object> diaryRedisTemplate) {
        this.diaryRedisTemplate = diaryRedisTemplate;
    }

    @Override
    public <T> Optional<List<T>> getList(String key, Class<T> elementType) {
        try {
            Object cachedValue = diaryRedisTemplate.opsForValue().get(key);
            if (cachedValue == null) {
                return Optional.empty();
            }
            if (!(cachedValue instanceof List<?> cachedList)) {
                log.warn("系统信息缓存类型不正确，key={}", key);
                return Optional.empty();
            }

            List<T> result = new ArrayList<>(cachedList.size());
            for (Object item : cachedList) {
                if (!elementType.isInstance(item)) {
                    log.warn("系统信息缓存元素类型不正确，key={}, expected={}", key, elementType.getName());
                    return Optional.empty();
                }
                result.add(elementType.cast(item));
            }
            return Optional.of(result);
        } catch (RuntimeException exception) {
            log.warn("读取系统信息缓存失败，key={}", key, exception);
            return Optional.empty();
        }
    }

    @Override
    public void putList(String key, List<?> values) {
        try {
            diaryRedisTemplate.opsForValue().set(key, new ArrayList<>(values));
        } catch (RuntimeException exception) {
            log.warn("写入系统信息缓存失败，key={}", key, exception);
        }
    }
}
