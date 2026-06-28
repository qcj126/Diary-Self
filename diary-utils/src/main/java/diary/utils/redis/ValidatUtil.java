package diary.utils.redis;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static diary.common.consts.RedisKeyConst.VERIFY_CODE_KEY;

/**
 * 校验工具类
 */
@Component
public class ValidatUtil {
    @Resource
    private RedisTemplate<String, Object> diaryRedisTemplate;

    // 将验证码存入redis
    public void setVerifyCode(String value) {
        diaryRedisTemplate.opsForValue().set(VERIFY_CODE_KEY, value, 5 * 60);
    }
    // 比对验证码，正确时删除验证码
    public boolean checkVerifyCode(String value) {
        String code = (String) diaryRedisTemplate.opsForValue().get(VERIFY_CODE_KEY);
        if (code != null && code.equals(value)) {
            diaryRedisTemplate.delete(VERIFY_CODE_KEY);
            return true;
        }
        return false;
    }
}
