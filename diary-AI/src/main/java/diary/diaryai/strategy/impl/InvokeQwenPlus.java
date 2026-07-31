package diary.diaryai.strategy.impl;


import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import diary.common.entity.ai.po.AiInfoPO;
import diary.common.entity.ai.po.AiNutrientPO;
import diary.common.enums.aienum.AIEnum;
import diary.diaryai.mapper.DiaryAIMapper;
import diary.diaryai.prompt.PromptContext;
import diary.diaryai.properties.AliCloudProperty;
import diary.diaryai.strategy.service.InvokeAIService;
import diary.diaryai.template.InvokeAITemplate;
import diary.utils.commonutil.MyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Order(1)
public class InvokeQwenPlus extends InvokeAITemplate implements InvokeAIService {
    private final AliCloudProperty aliCloudProperty;
    private final PromptContext promptContext;
    private final DiaryAIMapper diaryAIMapper;

    public InvokeQwenPlus(AliCloudProperty aliCloudProperty,
                          PromptContext promptContext,
                          DiaryAIMapper diaryAIMapper) {
        super(aliCloudProperty);
        this.promptContext = promptContext;
        this.aliCloudProperty = aliCloudProperty;
        this.diaryAIMapper = diaryAIMapper;
    }

    @Override
    public void invokeAI(Object data, Integer aiApplication, Integer aiType) {
        String model = aliCloudProperty.getQwenPlusModel();
        String prompt = buildPrompt(data);
        AiInfoPO aiInfoPO = AiInfoPO.builder()
                .id(MyUtils.getPrimaryKey())
                .userId(10000L)
                .temperature(aliCloudProperty.getTemperature().toString())
                .model(model)
                .aiType(aiType)
                .aiApplication(aiApplication)
                .build();
        diaryAIMapper.insertAiInfo(aiInfoPO);
        GenerationResult aiResult = constructRequest(prompt, model);
        List<Map<String, String>> resultList = extractResult(aiResult);
        log.info("AI返回的结果列表： {}", resultList);
        List<AiNutrientPO> aiNutrientPOS = new ArrayList<>();
        for (Map<String, String> result : resultList) {
            aiNutrientPOS.add(AiNutrientPO.builder()
                    .id(MyUtils.getPrimaryKey())
                    .userId(10000L)
                    .imageId(Long.parseLong(result.get("imageId")))
                    .aiInfoId(aiInfoPO.getId())
                    .calory(result.get("卡路里"))
                    .protein(result.get("蛋白质"))
                    .fat(result.get("脂肪"))
                    .carbohydrate(result.get("碳水化合物"))
                    .sugar(result.get("糖"))
                    .sodium(result.get("钠"))
                    .build());
        }
        diaryAIMapper.insertAiNutrient(aiNutrientPOS);
    }

    @Override
    public Integer getCode() {
        return AIEnum.QWENPLUS.getCode();
    }

    @Override
    public String buildPrompt(Object data) {
        return promptContext.getNutrientContent(data);
    }

    @Override
    public List<Map<String, String>> extractResult(GenerationResult aiResult) {
        String aiContent = aiResult.getOutput().getChoices().getFirst().getMessage().getContent();
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, String>>>(){}.getType();
        return gson.fromJson(aiContent, type);
    }
}
