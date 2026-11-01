package diary.diaryai.service;

import diary.common.entity.ai.vo.AiNutrientVO;

import java.util.List;

public interface AiNutrientQueryService {
    List<AiNutrientVO> getLatestDietNutrients(List<Long> dietIds, Long userId);
}
