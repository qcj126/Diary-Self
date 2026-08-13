package diary.diaryai.repository;

import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.po.AiInfoPO;
import diary.common.entity.ai.po.AiNutrientPO;
import diary.diaryai.mapper.DiaryAiMapper;
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
    private final DiaryAiMapper diaryAiMapper;

    /**
     * @param taskId
     * @param model
     * @param result
     * @param temperature
     * @param userId
     */
    @Transactional(rollbackFor = Exception.class)
    public void processData(Long taskId, Object data, String model, Map<String, String> result, Double temperature, Long userId) {
        AiInvokeDTO aiInvokeDTO = (AiInvokeDTO) data;
        AiInfoPO aiInfoPO = AiInfoPO.builder()
                .id(MyUtils.getPrimaryKey())
                .userId(10000L)
                .temperature(String.valueOf(temperature))
                .model(model)
                .aiType(aiInvokeDTO.getAiType())
                .aiApplication(aiInvokeDTO.getAiApplication())
                .build();
        diaryAiMapper.insertAiInfo(aiInfoPO);
        AiNutrientPO aiNutrientPO = AiNutrientPO.builder()
                .id(MyUtils.getPrimaryKey())
                .userId(10000L)
                .universalId(aiInvokeDTO.getUniversalId())
                .aiInfoId(aiInfoPO.getId())
                .calory(result.get("卡路里"))
                .protein(result.get("蛋白质"))
                .fat(result.get("脂肪"))
                .carbohydrate(result.get("碳水化合物"))
                .sugar(result.get("糖"))
                .sodium(result.get("钠"))
                .flag(aiInvokeDTO.getFlag())
                .build();

        diaryAiMapper.insertAiNutrient(aiNutrientPO);
        DiaryAiMapper.updateAiTaskStatus(taskId, "SUCCESS", aiInfoPO.getId(), userId, aiInvokeDTO.getClientRequestId());
    }
}
