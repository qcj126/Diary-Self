package diary.diaryai.rocketmqhandler.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.ai.dto.AiTaskMessageDto;
import diary.diaryai.mapper.DiaryAIMapper;
import diary.diaryai.service.AiTaskMessageProducer;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.apache.rocketmq.client.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTaskProducer implements AiTaskMessageProducer {
    private final RocketMQClientTemplate rocketMQClientTemplate;
    private final ObjectMapper objectMapper;
    private final DiaryAIMapper diaryAIMapper;

    @Override
    public SendReceipt send(AiTaskMessageDto aiTaskMessageDto) {
        try {
            String messageBody = objectMapper.writeValueAsString(aiTaskMessageDto);
            // 构建消息体，然后发送消息到Rocketmq
            Message<String> message = MessageBuilder
                    .withPayload(messageBody)
                    .setHeader(RocketMQHeaders.KEYS, aiTaskMessageDto.getTaskId())
                    .build();
            String destination = "diary-ai-task:QWEN_PLUS_NUTRIENT";
            return rocketMQClientTemplate.syncSendNormalMessage(destination, message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
