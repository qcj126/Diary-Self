package diary.diaryai.factory;

import diary.diaryai.strategy.service.InvokeAIService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AIFactory {
    @Resource
    private List<InvokeAIService> aiServiceList;

    private final ConcurrentHashMap<Integer, InvokeAIService> aiServiceCache = new ConcurrentHashMap<>();

    public AIFactory() {
        for (InvokeAIService exporter : this.aiServiceList) {
            Integer type = exporter.getCode();
            aiServiceCache.put(type, exporter);
            log.info("注册导出策略: {} -> {}", type, exporter.getClass().getSimpleName());
        }
    }

    /**
     * 根据类型代码获取导出策略
     */
    public InvokeAIService getAIService(Integer typeCode) {
        InvokeAIService exporter = aiServiceCache.get(typeCode);
        if (exporter == null) {
            throw new IllegalArgumentException(
                    String.format("不支持的导出类型: %s，支持的类型: %s",
                            typeCode, aiServiceCache.keySet())
            );
        }
        return exporter;
    }
}
