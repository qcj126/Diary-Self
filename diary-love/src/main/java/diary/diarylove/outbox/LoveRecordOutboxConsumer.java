//package diary.diarylove.outbox;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import diary.common.entity.love.dto.LoveRecordMessageDto;
//import diary.diarylove.properties.LoveRecordProperties;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.rocketmq.client.annotation.RocketMQMessageListener;
//import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
//import org.apache.rocketmq.client.apis.message.MessageView;
//import org.springframework.stereotype.Component;
//import org.apache.rocketmq.client.core.RocketMQListener;
//
//import java.nio.ByteBuffer;
//import java.util.Objects;
//
//import static diary.common.consts.MqTaskConst.OUTBOX_SCHEMA_VERSION;
//
//@Component
//@Slf4j
//@RequiredArgsConstructor
//@RocketMQMessageListener(
//        consumerGroup = "${diary.love.rocketmq.task-consumer-group:diary-love-record-worker}",
//        topic = "${diary.love.rocketmq.task-topic:diary-love-task}",
//        tag = "${diary.love.rocketmq.task-tag:LOVE_RECORD}",
//        sslEnabled = false
//)
//public class LoveRecordOutboxConsumer implements RocketMQListener {
//    private final ObjectMapper objectMapper;
//    private final LoveRecordProperties loveRecordProperties;
//
//    @Override
//    public ConsumeResult consume(MessageView messageView) {
//        // 获取outbox消息，然后将数据进行排序、表格排布处理
//        ByteBuffer body = messageView.getBody();
//        byte[] bytes = new byte[body.remaining()];
//        body.get(bytes);
//        final LoveRecordMessageDto message;
//        try {
//            message = objectMapper.readValue(bytes, LoveRecordMessageDto.class);
//            validateMessage(message);
//        } catch (Exception parseException) {
//            /*
//             * 以前只捕获 IOException，schemaVersion、taskId 等协议错误没有明确分类，可能直接穿透 Listener。
//             * 这类消息无法定位到可靠任务，返回 FAILURE 让 RocketMQ 按消费策略有限重试并最终进入 DLQ。
//             */
//            log.error("Invalid AI task message, messageId={}", messageView.getMessageId(), parseException);
//            return ConsumeResult.FAILURE;
//        }
//
//        // 处理排序、表格排布等逻辑
//
//
//        // 处理完毕之后，让notify模块向前端推送。
//        return ConsumeResult.SUCCESS;
//    }
//    // 再次校验消息，防止消息在传输过程中被篡改，或者其他功能的生产者误发消息到本消费者
//    // 也可以防止某些非法调用
//    private void validateMessage(LoveRecordMessageDto message) {
//        if (message == null
//                || message.getRecordId() == null
//                || message.getUserId() == null
//                || message.getClientRequestId() == null
//                || message.getClientRequestId().isBlank()
//                || !Objects.equals(OUTBOX_SCHEMA_VERSION, message.getSchemaVersion())
//                || !loveRecordProperties.getRocketmq().getTaskTag().equals(message.getTaskType())) {
//            throw new IllegalArgumentException("LoveRecord消息字段或协议版本不合法");
//        }
//    }
//}
