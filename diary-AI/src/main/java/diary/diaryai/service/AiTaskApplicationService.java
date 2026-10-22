package diary.diaryai.service;

import diary.diaryai.entity.ai.dto.AiInvokeDTO;
import diary.diaryai.entity.ai.vo.AiTaskSubmitVo;

public interface AiTaskApplicationService {

    AiTaskSubmitVo submitNutrientAnalyze(AiInvokeDTO aiInvokeDTO, Long userId);

}
