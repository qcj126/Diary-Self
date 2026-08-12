package diary.diaryai.mapper;

import diary.common.entity.ai.po.AiInfoPO;
import diary.common.entity.ai.po.AiNutrientPO;
import diary.common.entity.ai.po.AiTaskPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiaryAIMapper {
    void insertAiNutrient(AiNutrientPO aiNutrientPO);

    void insertAiInfo(AiInfoPO aiInfoPO);

    int updateAiTaskStatus(Long taskId, String success, Long id);

    void insertAiTask(AiTaskPO aiTaskPO);

    AiTaskPO selectAiTaskByTaskId(Long taskId);
}
