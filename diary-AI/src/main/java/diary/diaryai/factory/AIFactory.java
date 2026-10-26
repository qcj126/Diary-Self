package diary.diaryai.factory;

import diary.diaryai.service.InvokeNutrientAiService;
import diary.diaryai.service.InvokeLoveRecordAIService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIFactory {
    private final List<InvokeNutrientAiService> aiNutrientServiceList;
    private final List<InvokeLoveRecordAIService> aiLoveRecordServiceList;

    private final Map<Integer, InvokeNutrientAiService> aiNutrientServiceCache = new HashMap<>();
    private final Map<Integer, InvokeLoveRecordAIService> aiLoveRecordServiceCache = new HashMap<>();

    @PostConstruct
    public void init() {
        for (InvokeNutrientAiService service : aiNutrientServiceList) {
            Integer type = service.getCode();
            if (type == null) {
                throw new IllegalStateException("AI模型编码不能为null: "
                        + service.getClass().getSimpleName());
            }
            InvokeNutrientAiService existing = aiNutrientServiceCache.putIfAbsent(type, service);
            if (existing != null) {
                throw new IllegalStateException("AI模型编码重复: " + type
                        + " (" + existing.getClass().getSimpleName()
                        + ", " + service.getClass().getSimpleName() + ")");
            }
            log.info("注册营养分析AI: {} -> {}", type, service.getClass().getSimpleName());
        }
        for (InvokeLoveRecordAIService service : aiLoveRecordServiceList) {
            Integer type = service.getCode();
            if (type == null) {
                throw new IllegalStateException("爱情情感AI模型编码不能为null: "
                        + service.getClass().getSimpleName());
            }

            InvokeLoveRecordAIService existing =
                    aiLoveRecordServiceCache.putIfAbsent(type, service);
            if (existing != null) {
                throw new IllegalStateException("爱情情感AI模型编码重复: " + type);
            }

            log.info("注册爱情情感AI: {} -> {}", type, service.getClass().getSimpleName());
        }
    }

    /**
     * 根据类型获取AI实现类
     */
    public InvokeNutrientAiService getNutrientAIService(Integer aiType) {
        InvokeNutrientAiService exporter = aiNutrientServiceCache.get(aiType);
        if (exporter == null) {
            throw new IllegalArgumentException(
                    String.format("不支持的AI模型编码: %s，已注册编码: %s",
                            aiType, aiLoveRecordServiceCache.keySet())
            );
        }
        return exporter;
    }

    public InvokeLoveRecordAIService getLoveRecordAIService(Integer aiType) {
        InvokeLoveRecordAIService exporter = aiLoveRecordServiceCache.get(aiType);
        if (exporter == null) {
            throw new IllegalArgumentException(
                    String.format("不支持的AI模型编码: %s，已注册编码: %s",
                            aiType, aiLoveRecordServiceCache.keySet())
            );
        }
        return exporter;
    }
}
