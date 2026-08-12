package diary.diaryai.repository;

import diary.common.entity.ai.po.AiInfoPO;
import diary.common.entity.ai.po.AiNutrientPO;
import diary.diaryai.mapper.DiaryAIMapper;
import diary.diaryai.properties.AliCloudProperty;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

// 专门处理数据库事务的类
@Service
@Slf4j
@RequiredArgsConstructor
public class DatabaseServiceImpl {
    private final DiaryAIMapper diaryAIMapper;

    /**
     * @param aiApplication
     * @param aiType
     * @param flag
     * @param taskId
     * @param universalId
     * @param model
     * @param result
     * @param temperature
     */
    @Transactional(rollbackFor = Exception.class)
    public void processData(Integer aiApplication, Integer aiType, String flag, Long taskId, Long universalId, String model, Map<String, String> result, Double temperature) {
        AiInfoPO aiInfoPO = AiInfoPO.builder()
                .id(MyUtils.getPrimaryKey())
                .userId(10000L)
                .temperature(String.valueOf(temperature))
                .model(model)
                .aiType(aiType)
                .aiApplication(aiApplication)
                .build();
        diaryAIMapper.insertAiInfo(aiInfoPO);
        AiNutrientPO aiNutrientPO = AiNutrientPO.builder()
                .id(MyUtils.getPrimaryKey())
                .userId(10000L)
                .universalId(universalId)
                .aiInfoId(aiInfoPO.getId())
                .calory(result.get("卡路里"))
                .protein(result.get("蛋白质"))
                .fat(result.get("脂肪"))
                .carbohydrate(result.get("碳水化合物"))
                .sugar(result.get("糖"))
                .sodium(result.get("钠"))
                .flag(flag)
                .build();

        diaryAIMapper.insertAiNutrient(aiNutrientPO);
        diaryAIMapper.updateAiTaskStatus(taskId, "SUCCESS", aiInfoPO.getId());
    }

}
