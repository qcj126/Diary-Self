package diary.diaryai.impl;

import diary.diaryai.entity.ai.dto.LoveRecordSentimentSubmitDTO;
import diary.diaryai.entity.ai.vo.AiTaskSubmitVo;
import diary.diaryai.service.AiTaskSentimentApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiTaskSentimentApplicationServiceImpl implements AiTaskSentimentApplicationService {
    @Override
    public AiTaskSubmitVo submitLoveRecordSentimentAnalyze(LoveRecordSentimentSubmitDTO loveRecordSentimentSubmitDTO, Long userId) {

        return null;
    }
}
