package diary.diaryai.factory;

import diary.diaryai.strategy.service.InvokeAIService;
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
    private final List<InvokeAIService> aiServiceList;

    private final Map<Integer, InvokeAIService> aiServiceCache = new HashMap<>();

    @PostConstruct
    public void init() {
        for (InvokeAIService service : aiServiceList) {
            Integer type = service.getCode();
            if (type == null) {
                throw new IllegalStateException("AI模型编码不能为null: "
                        + service.getClass().getSimpleName());
            }
            InvokeAIService existing = aiServiceCache.putIfAbsent(type, service);
            if (existing != null) {
                throw new IllegalStateException("AI模型编码重复: " + type
                        + " (" + existing.getClass().getSimpleName()
                        + ", " + service.getClass().getSimpleName() + ")");
            }
            log.info("注册AI: {} -> {}", type, service.getClass().getSimpleName());
        }
    }

    /**
     * 根据类型获取AI实现类
     */
    public InvokeAIService getAIService(Integer typeCode) {
        InvokeAIService exporter = aiServiceCache.get(typeCode);
        if (exporter == null) {
            throw new IllegalArgumentException(
                    String.format("不支持的AI模型编码: %s，已注册编码: %s",
                            typeCode, aiServiceCache.keySet())
            );
        }
        return exporter;
    }
}
