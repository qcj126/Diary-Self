package diary.diaryai.mapper;

import diary.common.entity.ai.po.AiInfoPO;
import diary.common.entity.ai.po.AiNutrientPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface DiaryAIMapper {
    void insertAiNutrient(List<AiNutrientPO> aiNutrientPOs);

    void insertAiInfo(AiInfoPO aiInfoPO);
}
