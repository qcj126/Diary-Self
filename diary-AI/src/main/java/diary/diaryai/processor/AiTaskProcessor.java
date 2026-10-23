package diary.diaryai.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.diaryai.entity.ai.dto.AiInvokeDTO;
import diary.diaryai.entity.ai.dto.AiTaskMessageDto;
import diary.diaryai.entity.ai.dto.LoveRecordSentimentInputSnapshotDTO;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.factory.AIFactory;
import diary.diaryai.service.AiTaskProcessorService;
import diary.diaryai.service.InvokeNutrientAiService;
import diary.diaryai.service.InvokeLoveRecordAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTaskProcessor implements AiTaskProcessorService {
    private final ObjectMapper objectMapper;
    private final AIFactory aiFactory;
    @Override
    public void NutrientTaskProcessor(String inputSnapshot, AiTaskMessageDto message, AiTaskPO aiTaskPO) {
        try {
            AiInvokeDTO snapshotDTO = objectMapper.readValue(inputSnapshot, AiInvokeDTO.class);
            InvokeNutrientAiService aiService = aiFactory.getNutrientAIService(snapshotDTO.getAiType());
            aiService.getNutrientAiResultAndSave(
                    snapshotDTO,
                    message.getTaskId(),
                    message.getUserId(),
                    aiTaskPO.getWorkerId(),
                    aiTaskPO.getVersionId()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("processor中反序列化快照失败");
        }
    }

    @Override
    public void SentimentTaskProcessor(String inputSnapshot, AiTaskMessageDto message, AiTaskPO aiTaskPO) {
        try {
            LoveRecordSentimentInputSnapshotDTO snapshotDTO = objectMapper.readValue(inputSnapshot, LoveRecordSentimentInputSnapshotDTO.class);
            InvokeLoveRecordAIService aiService = aiFactory.getLoveRecordAIService(snapshotDTO.getAiType());
            aiService.getLoveRecordAiResultAndSave(
                    snapshotDTO,
                    message.getTaskId(),
                    message.getUserId(),
                    aiTaskPO.getClientRequestId(),
                    aiTaskPO.getWorkerId(),
                    aiTaskPO.getVersionId()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("processor中反序列化快照失败");
        }
    }
}
