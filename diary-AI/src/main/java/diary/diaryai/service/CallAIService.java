package diary.diaryai.service;

import diary.common.entity.ai.dto.AiInvokeDTO;

public interface CallAIService {
    void callAI(AiInvokeDTO aiInvokeDTO);
}
