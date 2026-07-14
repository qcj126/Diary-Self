package diary.diaryai.strategy.impl;

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import diary.common.enums.aienum.AIEnum;
import diary.diaryai.properties.AliCloudProperty;
import diary.diaryai.strategy.service.InvokeAIService;
import diary.diaryai.template.InvokeAITemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
public class InvokeDeepSeek extends InvokeAITemplate implements InvokeAIService {
    private final AliCloudProperty aliCloudProperty;

    public InvokeDeepSeek(AliCloudProperty aliCloudProperty) {
        super(aliCloudProperty);
        this.aliCloudProperty = aliCloudProperty;
    }

    @Override
    public Object invokeAI(Object data) {
        String model = aliCloudProperty.getDeepSeekModel();
        String prompt = buildPrompt();
        GenerationResult aiResult = constructRequest(prompt, model);
        return extractResult(aiResult);
    }

    @Override
    public Integer getCode() {
        return AIEnum.DEEPSEEK.getCode();
    }

    @Override
    public String buildPrompt() {
        return null;
    }

    @Override
    public Object extractResult(GenerationResult aiResult) {
        return aiResult.getOutput().getChoices().getFirst().getMessage().getContent();
    }
}
