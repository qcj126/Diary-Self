package diary.file.service.mqservice;

import com.rabbitmq.client.Channel;
import diary.file.entity.po.OssUploadSuccessMsg;
import org.springframework.amqp.core.Message;

public interface MqConsumerService {
    void handleUploadSuccess(OssUploadSuccessMsg message, Message amqpMessage, Channel channel);

    void handleDeadLetter(OssUploadSuccessMsg message, Message amqpMessage, Channel channel);
}
