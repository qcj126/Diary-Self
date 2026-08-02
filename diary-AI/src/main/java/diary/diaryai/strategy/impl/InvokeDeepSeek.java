package diary.diaryai.strategy.impl;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import diary.common.enums.aienum.AIEnum;
import diary.diaryai.mapper.DiaryAIMapper;
import diary.diaryai.prompt.PromptContext;
import diary.diaryai.properties.AliCloudProperty;
import diary.diaryai.strategy.service.InvokeAIService;
import diary.diaryai.template.InvokeAITemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Order(1)
public class InvokeDeepSeek extends InvokeAITemplate implements InvokeAIService {
    private final AliCloudProperty aliCloudProperty;
    private final PromptContext promptContext;
    private final DiaryAIMapper diaryAIMapper;

    public InvokeDeepSeek(AliCloudProperty aliCloudProperty,
                          PromptContext promptContext,
                          DiaryAIMapper diaryAIMapper) {
        this.promptContext = promptContext;
        this.aliCloudProperty = aliCloudProperty;
        this.diaryAIMapper = diaryAIMapper;
    }

    @Override
    public void getAiResultAndSave(Object data, Integer aiApplication, Integer aiType) {
//        String model = aliCloudProperty.getDeepSeekModel();
//        Object prompt = buildPrompt(data);
//        AiInfoPO aiInfoPO = AiInfoPO.builder()
//                .id(MyUtils.getPrimaryKey())
//                .userId(10000L)
//                .temperature(aliCloudProperty.getTemperature().toString())
//                .model(model)
//                .aiType(aiType)
//                .aiApplication(aiApplication)
//                .build();
//        diaryAIMapper.insertAiInfo(aiInfoPO);
//        GenerationResult aiResult = invokeAi(prompt, model);
//        List<Map<String, String>> resultList = extractResult(aiResult);
//        log.info("AI返回的结果列表： {}", resultList);
//        List<AiNutrientPO> aiNutrientPOS = new ArrayList<>();
//        for (Map<String, String> result : resultList) {
//            aiNutrientPOS.add(AiNutrientPO.builder()
//                    .id(MyUtils.getPrimaryKey())
//                    .userId(10000L)
//                    .imageId(Long.parseLong(result.get("imageId")))
//                    .aiInfoId(aiInfoPO.getId())
//                    .calory(result.get("卡路里"))
//                    .protein(result.get("蛋白质"))
//                    .fat(result.get("脂肪"))
//                    .carbohydrate(result.get("碳水化合物"))
//                    .sugar(result.get("糖"))
//                    .sodium(result.get("钠"))
//                    .build());
//        }
//        diaryAIMapper.insertAiNutrient(aiNutrientPOS);
    }

    @Override
    public Integer getCode() {
        return AIEnum.DEEPSEEK.getCode();
    }

    @Override
    public String buildPrompt(Object data) {
//        return promptContext.getNutrientContent(data);
        return null;
    }

    @Override
    public MultiModalConversationResult invokeAi(Object prompt, String model) {
        return null;
    }

    @Override
    public List<Map<String, String>> extractResult(Object aiResult, String model, Object prompt) {
//        String aiContent = aiResult.getOutput().getChoices().getFirst().getMessage().getContent();
//        Gson gson = new Gson();
//        Type type = new TypeToken<List<Map<String, String>>>(){}.getType();
//        return gson.fromJson(aiContent, type);
        return null;
    }
}
