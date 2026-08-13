package diary.diaryai.rocketmqhandler.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.ai.dto.AiTaskMessageDto;
import diary.common.entity.ai.dto.AiTaskProcessDto;
import diary.diaryai.executor.AiTaskExecutor;
import diary.diaryai.mapper.DiaryAiMapper;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.annotation.RocketMQMessageListener;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.core.RocketMQListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Inet4Address;
import java.nio.ByteBuffer;

@Service
@RocketMQMessageListener(
        consumerGroup = "diary-ai-qwen-plus-worker-v1",
        topic = "diary-ai-task",
        tag = "QWEN_PLUS_NUTRIENT"
)
@RequiredArgsConstructor
public class AiTaskConsumer implements RocketMQListener {
    private final ObjectMapper objectMapper;
    private final AiTaskExecutor aiTaskExecutor;
    private final DiaryAiMapper diaryAiMapper;
    @Override
    public ConsumeResult consume(MessageView messageView) {
        ByteBuffer bodyBuffer = messageView.getBody();
        byte[] body = new byte[bodyBuffer.remaining()];
        bodyBuffer.get(body);

        try {
            AiTaskMessageDto message = objectMapper.readValue(body, AiTaskMessageDto.class);
            // 更新状态机
            AiTaskProcessDto aiTaskProcessDto = AiTaskProcessDto.builder()
                    .taskId(message.getTaskId())
                    .userId(message.getUserId())
                    .clientRequestId(message.getClientRequestId())
                    .workerId("diary-ai" + Inet4Address.getLocalHost().getHostName() + System.currentTimeMillis())
                    .status("QUEUED")
                    .build();
            diaryAiMapper.updateAiTaskStatus(aiTaskProcessDto);
            aiTaskExecutor.execute(message);
            return ConsumeResult.SUCCESS;
        } catch (IOException e) {
            return ConsumeResult.FAILURE;
        }
    }
}
