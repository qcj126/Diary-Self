//package diary.diarylove.outbox;
//
//import diary.common.entity.mq.po.MqOutboxPO;
//import diary.diarylove.mapper.DiaryLoveMapper;
//import diary.diarylove.properties.LoveRecordProperties;
//import diary.diarylove.service.LoveRecordOutboxService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.rocketmq.client.apis.producer.SendReceipt;
//import org.apache.rocketmq.client.core.RocketMQClientTemplate;
//import org.apache.rocketmq.client.support.RocketMQHeaders;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.support.MessageBuilder;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class LoveRecordOutboxPublisher {
//    private final DiaryLoveMapper diaryLoveMapper;
//    private final LoveRecordProperties properties;
//    private final LoveRecordOutboxService outboxService;
//    private final RocketMQClientTemplate rocketMQClientTemplate;
//
//    @Scheduled(fixedDelayString = "${diary.love.rocketmq.publisher-interval-ms:5000}")
//    public void publishReadyMessages() {
//        // TODO 回头先像diary-AI模块那样，先恢复超时的消息，此时暂时先拉通流程
//        List<MqOutboxPO> mqOutboxPOS = diaryLoveMapper.selectReadyOutbox(properties.getRocketmq().getPublisherBatchSize());
//        // 改变outbox状态，将new 或retry_wait改为sending
//        for (MqOutboxPO mqOutboxPO : mqOutboxPOS) {
//            if (!outboxService.claim(mqOutboxPO)) {
//                continue;
//            }
//            // 获取outbox消息并发送到broker
//            Message<String> message = MessageBuilder.withPayload(mqOutboxPO.getPayload())
//                    .setHeader(RocketMQHeaders.KEYS, mqOutboxPO.getMessageKey())
//                    .build();
//            String destination = mqOutboxPO.getTopic() + ":" + mqOutboxPO.getTag();
//            SendReceipt sendReceipt = rocketMQClientTemplate.syncSendNormalMessage(destination, message);
//            log.info("Sent message, messageId: {}", sendReceipt.getMessageId());
//            // 发送成功，将outbox状态以及消息id记录到outbox中
//            outboxService.confirmSent(mqOutboxPO, sendReceipt.getMessageId().toString());
//        }
//    }
//}
