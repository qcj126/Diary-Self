package diary.diaryai.service;

import diary.common.entity.ai.dto.AIInvokeDTO;

import java.io.FileNotFoundException;

public interface CallAIService {
    void callAI(AIInvokeDTO aiInvokeDTO) throws FileNotFoundException;
}
