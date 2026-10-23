package diary.diaryai.service;

import diary.diaryai.entity.ai.dto.AiTaskMessageDto;
import diary.diaryai.entity.ai.po.AiTaskPO;

public interface AiTaskProcessorService {
    void NutrientTaskProcessor(String inputSnapshot, AiTaskMessageDto message, AiTaskPO aiTaskPO);
    void SentimentTaskProcessor(String inputSnapshot, AiTaskMessageDto message, AiTaskPO aiTaskPO);
}
