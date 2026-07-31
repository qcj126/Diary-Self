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
    public void invokeAI(Object data) {
        String model = aliCloudProperty.getDeepSeekModel();
        String prompt = buildPrompt(data);
        GenerationResult aiResult = constructRequest(prompt, model);
        extractResult(aiResult);
    }

    @Override
    public Integer getCode() {
        return AIEnum.DEEPSEEK.getCode();
    }

    @Override
    public String buildPrompt(Object data) {
        return "这是我的一餐，请你帮我分析其中的营养成分，只分析糖类、蛋白质、脂肪、碳水化合物的含量，并给出每种营养成分的含量和总热量。" +
                "我的图片文件流数据，是map格式，键为图片id，值为文件流：" + data +
                "请以JSON格式返回结果，格式如下：{\"calories\": \"xx kcal\", \"protein\": \"xx g\", \"fat\": \"xx g\", \"carbohydrates\": \"xx g\"}。";
    }

    @Override
    public Object extractResult(GenerationResult aiResult) {
        return aiResult.getOutput().getChoices().getFirst().getMessage().getContent();
    }
}
