package diary.diarygoal.factory;

import diary.diarygoal.strategy.service.Exporter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ExporterFactory {
    // 使用 Spring 自动注入所有 Exporter 实现
    @Resource
    private List<Exporter> exporterList;

    // 策略缓存 Map (类型 -> 策略实例)
    private final Map<Integer, Exporter> exporterCache = new ConcurrentHashMap<>();

    public ExporterFactory() {}

    @PostConstruct
    public void init() {
        if (exporterList != null) {
            for (Exporter exporter : this.exporterList) {
                Integer type = exporter.getCode();
                exporterCache.put(type, exporter);
                log.info("注册导出策略: {} -> {}", type, exporter.getClass().getSimpleName());
            }
        } else {
            log.warn("exporterList is null in ExporterFactory@PostConstruct");
        }
    }

    /**
     * 根据类型代码获取导出策略
     */
    public Exporter getExporter(Integer typeCode) {
        Exporter exporter = exporterCache.get(typeCode);
        if (exporter == null) {
            throw new IllegalArgumentException(
                    String.format("不支持的导出类型: %s，支持的类型: %s",
                            typeCode, exporterCache.keySet())
            );
        }
        return exporter;
    }
}
