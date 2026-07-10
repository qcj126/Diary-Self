package diary.diaryai.strategy.impl;

import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import diary.common.enums.aienum.AIEnum;
import diary.diaryai.properties.AliCloudProperty;
import diary.diaryai.strategy.service.InvokeAIService;
import diary.diaryai.template.InvokeAITemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class InvokeDeepSeek extends InvokeAITemplate implements InvokeAIService {
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
        return AIEnum.DEEPSEEK.getCode();
    }

    @Override
    public String buildPrompt() {
        return null;
    }

    @Override
    public Object callAI(Object data) {


        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("You are a helpful assistant.")
                .build();
        return null;
    }

    @Override
    public Object extractResult(Object aiResult) {

        return null;
    }
}
