package diary.diaryai.strategy.impl;

import diary.common.enums.aienum.AIEnum;
import diary.diaryai.properties.AliCloudProperty;
import diary.diaryai.strategy.service.InvokeAIService;
import diary.diaryai.template.InvokeAITemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class InvokeDbao extends InvokeAITemplate implements InvokeAIService {
    private final AliCloudProperty aliCloudProperty;
    @Override
    public Object invokeAI(Object data) {
        String model = aliCloudProperty.getModel();
        String prompt = buildPrompt();
        Object request = constructRequest(prompt, model);
        Object aiResult = callAI(request);
        return extractResult(aiResult);
    }

    @Override
    public Integer getCode() {
        return AIEnum.DBAO.getCode();
    }

    @Override
    public String buildPrompt() {
        return null;
    }

    @Override
    public Object callAI(Object data) {
        return null;
    }

    @Override
    public Object extractResult(Object aiResult) {
        return null;
    }
}
