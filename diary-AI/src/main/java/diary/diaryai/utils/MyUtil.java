package diary.diaryai.utils;

import diary.common.exception.NullResultException;
import diary.diaryai.properties.AiTaskProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MyUtil {
    private final AiTaskProperties properties;

    public String getOutboxTag(Integer aiApplication) {
        switch (aiApplication) {
            case 1 -> {
                return properties.getRocketmq().getTaskNutrientTag();
            }
            case 2 -> {
                return properties.getRocketmq().getTaskSentimentTag();
            }
            default -> throw new NullResultException("不存在此AI用途");
        }
    }
}
