package diary.diaryai.redis;

import diary.diaryai.enums.aienum.AIEnum;
import diary.diaryai.properties.AiTaskProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AiRedisKeyFactory {
    private final AiTaskProperties properties;

    public String submitRate(Long userId, long epochMinute) {
        return properties.getRedis().getKeyPrefix() + ":submit:rate:" + userId + ":" + epochMinute;
    }

    public String globalModelPermit(AIEnum model) {
        Objects.requireNonNull(model, "model must not be null");
        return properties.getRedis().getKeyPrefix() + ":global-permit:model:" + model.name();
    }
}
