package diary.diaryai.service;

import diary.diaryai.entity.ai.dto.LoveRecordSentimentSubmitDTO;
import diary.diaryai.entity.ai.vo.AiTaskSubmitVo;

public interface AiTaskSentimentApplicationService {
    AiTaskSubmitVo submitLoveRecordSentimentAnalyze(LoveRecordSentimentSubmitDTO loveRecordSentimentSubmitDTO, Long userId);
}
