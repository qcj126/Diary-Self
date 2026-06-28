package diary.utils.redis;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    @Resource
    private RedisTemplate<String, Object> diaryRedisTemplate;

    /**
     * 无过期时间缓存
     * @param key
     * @param value
     */
    public void setString(String key, String value) {
        diaryRedisTemplate.opsForValue().set(key, value);
    }
    public String getString(String key) {
        return (String) diaryRedisTemplate.opsForValue().get(key);
    }
    public void deleteString(String key) {
        diaryRedisTemplate.delete(key);
    }
    /**
     * 过期时间缓存
     * @param key
     * @param value
     * @param expire
     */
    public void setStringWithExpire(String key, String value, long expire) {
        diaryRedisTemplate.opsForValue().set(key, value, expire, TimeUnit.MINUTES);
    }
    public String getStringWithExpire(String key) {
        return (String) diaryRedisTemplate.opsForValue().get(key);
    }
}
