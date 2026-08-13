package diary.diaryai.mapper;

import diary.common.entity.ai.dto.AiTaskProcessDto;
import diary.common.entity.ai.po.AiInfoPO;
import diary.common.entity.ai.po.AiNutrientPO;
import diary.common.entity.ai.po.AiTaskPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiaryAiMapper {
    void insertAiNutrient(AiNutrientPO aiNutrientPO);

    void insertAiInfo(AiInfoPO aiInfoPO);

    void insertAiTask(AiTaskPO aiTaskPO);

    AiTaskPO selectAiTaskByTaskId(Long taskId);

    void updateAiTaskStatus(AiTaskProcessDto aiTaskProcessDto);
}
