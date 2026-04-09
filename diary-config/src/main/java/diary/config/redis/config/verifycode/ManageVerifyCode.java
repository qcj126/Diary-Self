package diary.config.redis.config.verifycode;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ManageVerifyCode {
    public static final String VERIFY_CODE_KEY = "user:login:verifyCode: ";

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
