package diary.diaryai.service;

import diary.diaryai.entity.ai.dto.AiInvokeDTO;
import diary.diaryai.entity.ai.vo.AiTaskSubmitVo;

public interface AiTaskNutrientApplicationService {

    AiTaskSubmitVo submitNutrientAnalyze(AiInvokeDTO aiInvokeDTO, Long userId);

}
