//package diary.diaryai.strategy.nutrientanlalyze;
//
//import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
//import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
//import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
//import com.alibaba.dashscope.common.MultiModalMessage;
//import com.alibaba.dashscope.common.Role;
//import com.alibaba.dashscope.utils.Constants;
//import com.google.gson.Gson;
//import com.google.gson.reflect.TypeToken;
//import diary.common.entity.ai.po.AiInfoPO;
//import diary.common.entity.ai.po.AiNutrientPO;
//import diary.common.enums.aienum.AIEnum;
//import diary.common.exception.CustomException;
//import diary.diaryai.mapper.DiaryAiMapper;
//import diary.diaryai.prompt.PromptContext;
//import diary.diaryai.properties.AliCloudProperty;
//import diary.diaryai.strategy.service.InvokeAIService;
//import diary.diaryai.template.InvokeAITemplate;
//import diary.utils.commonutil.MyUtils;
//import diary.utils.redis.RedisUtil;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import java.lang.reflect.Type;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//
//@Component
//@Order(1)
//@Slf4j
//@RequiredArgsConstructor
//public class InvokeQwenFlash extends InvokeAITemplate implements InvokeAIService {
//    private final AliCloudProperty aliCloudProperty;
//    private final PromptContext promptContext;
//    private final DiaryAiMapper diaryAiMapper;
//    private final RedisUtil redisUtil;
//    private final MultiModalConversation conv = new MultiModalConversation();
//
//    @Override
//    public void getAiResultAndSave(Object data, Long taskId, Long userId) {
//        String model = aliCloudProperty.getQwenPlusModel();
//        Map<Long, String> dataMap = (Map<Long, String>) data;
//        for (Map.Entry<Long, String> entry : dataMap.entrySet()) {
//            Long imageId = entry.getKey();
//            String imageUrl = entry.getValue();
//            try {
//                URI uri = new URI(imageUrl);
//                String objectKey = uri.getPath().substring(1);
//                redisUtil.setString(objectKey, imageId);
//            } catch (URISyntaxException e) {
//                throw new CustomException("图片URL格式错误");
//            }
//        }
//        Object prompt = buildPrompt(data);
//        AiInfoPO aiInfoPO = AiInfoPO.builder()
//                .id(MyUtils.getPrimaryKey())
//                .userId(10000L)
//                .temperature(aliCloudProperty.getTemperature().toString())
//                .model(model)
//                .aiType(aiType)
//                .aiApplication(aiApplication)
//                .build();
//        diaryAiMapper.insertAiInfo(aiInfoPO);
//        MultiModalConversationResult aiResult = invokeAi(prompt, model);
//        List<Map<String, String>> resultList = extractResult(aiResult, model, prompt);
//        log.info("AI返回的结果列表： {}", resultList);
//        List<AiNutrientPO> aiNutrientPOS = new ArrayList<>();
//        for (Map<String, String> result : resultList) {
//            Object imageId = redisUtil.getString(result.get("objectKey"));
//            aiNutrientPOS.add(AiNutrientPO.builder()
//                    .id(MyUtils.getPrimaryKey())
//                    .userId(10000L)
//                    .universalId(Long.parseLong(imageId.toString()))
//                    .aiInfoId(aiInfoPO.getId())
//                    .calory(result.get("卡路里"))
//                    .protein(result.get("蛋白质"))
//                    .fat(result.get("脂肪"))
//                    .carbohydrate(result.get("碳水化合物"))
//                    .sugar(result.get("糖"))
//                    .sodium(result.get("钠"))
//                    .build());
//            redisUtil.deleteString(result.get("objectKey"));
//        }
//        diaryAiMapper.insertAiNutrient(aiNutrientPOS);
//    }
//
//    @Override
//    public Integer getCode() {
//        return AIEnum.QWENPLUS.getCode();
//    }
//
//    @Override
//    public List<Map<String, Object>> buildPrompt(Object data) {
//        return promptContext.getNutrientContentByModelQwenPlusAndFlash(data);
//    }
//
//    @Override
//    public MultiModalConversationResult invokeAi(Object prompt, String model) {
//        Constants.baseHttpApiUrl = aliCloudProperty.getUrl();
//        String apiKey = aliCloudProperty.getApiKey();
//        Double temperature = aliCloudProperty.getTemperature();
//        try {
//            List<Map<String, Object>> userMsg = (List<Map<String, Object>>) prompt;
//            MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
//                    .content(userMsg).build();
//
//            MultiModalConversationParam param = MultiModalConversationParam.builder()
//                    .apiKey(apiKey)
//                    .model("qwen3.7-flash")
//                    .temperature(temperature.floatValue())
//                    .messages(Collections.singletonList(userMessage))
//                    .build();
//            return conv.call(param);
//        } catch (Exception e) {
//            throw new CustomException("调用Qwen3.7-Plus模型失败：" + e.getMessage());
//        }
//    }
//
//    @Override
//    public List<Map<String, String>> extractResult(Object aiResult, String model, Object prompt) {
//        String aiContent = ((MultiModalConversationResult) aiResult).getOutput().getChoices().getFirst().getMessage().getContent().getFirst().get("text").toString();
//        Gson gson = new Gson();
//        Type type = new TypeToken<List<Map<String, String>>>(){}.getType();
//        return gson.fromJson(aiContent, type);
//    }
//}
