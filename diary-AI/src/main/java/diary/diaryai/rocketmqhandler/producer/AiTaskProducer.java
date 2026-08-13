package diary.diaryai.rocketmqhandler.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.ai.dto.AiTaskMessageDto;
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

    @Override
    public SendReceipt send(AiTaskMessageDto aiTaskMessageDto) {
        try {
            String messageBody = objectMapper.writeValueAsString(aiTaskMessageDto);
            /*
             * 以前直接把 Long 放入 KEYS Header，不同客户端转换行为可能不一致，排查消息时也不直观。
             * 现在显式转换成 taskId 字符串，保证 RocketMQ Message Key 稳定且可按任务查询。
             */
            Message<String> message = MessageBuilder
                    .withPayload(messageBody)
                    .setHeader(RocketMQHeaders.KEYS, aiTaskMessageDto.getTaskId().toString())
                    .build();
            String destination = "diary-ai-task:QWEN_PLUS_NUTRIENT";
            return rocketMQClientTemplate.syncSendNormalMessage(destination, message);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("AI任务消息序列化失败", e);
        }
    }
}
