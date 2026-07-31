package diary.diaryai.strategy.impl;

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import diary.common.enums.aienum.AIEnum;
import diary.diaryai.properties.AliCloudProperty;
import diary.diaryai.strategy.service.InvokeAIService;
import diary.diaryai.template.InvokeAITemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Map;

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
        Map<String, String> map = extractResult(aiResult);
        // 将数据进行入库处理
    }

    @Override
    public Integer getCode() {
        return AIEnum.DEEPSEEK.getCode();
    }

    @Override
    public String buildPrompt(Object data) {
        return
                "这是我的一餐，请你帮我分析其中的营养成分，只分析糖类、蛋白质、脂肪、碳水化合物的含量，并给出每种营养成分的含量和总热量。" +
                "务必以JSON格式返回结果，格式如下：{\"卡路里\": \"xx kcal\", \"蛋白质\": \"xx g\", \"脂肪\": \"xx g\", \"碳水化合物\": \"xx g\", \"糖\": \"xx g\"}。" +
                "我的图片文件流数据，是map格式，键为图片id，值为文件流：" + data;
    }

    @Override
    public Map<String, String> extractResult(GenerationResult aiResult) {
        String aiContent = aiResult.getOutput().getChoices().getFirst().getMessage().getContent();
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        return gson.fromJson(aiContent, type);
    }
}
