package diary.diaryai.redis;

import diary.diaryai.properties.AiTaskProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiRedisKeyFactory {
    private final AiTaskProperties properties;

    public String submitRate(Long userId, long epochMinute) {
        return properties.getRedis().getKeyPrefix() + ":submit:rate:" + userId + ":" + epochMinute;
    }
}
