package diary.diaryai.service;

import diary.common.entity.ai.dto.AiTaskMessageDto;
import org.apache.rocketmq.client.apis.producer.SendReceipt;

public interface AiTaskMessageProducer {
    SendReceipt send(AiTaskMessageDto aiTaskMessageDto);
}
