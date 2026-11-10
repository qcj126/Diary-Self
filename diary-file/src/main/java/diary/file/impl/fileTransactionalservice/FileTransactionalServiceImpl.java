package diary.file.impl.fileTransactionalservice;

import diary.file.entity.mq.po.FileMqOutboxPO;
import diary.file.enums.ImageTransitionResultEnum;
import diary.file.mapper.FileMqOutboxMapper;
import diary.file.mq.event.ImageUploadedEvent;
import diary.file.property.FileMqProperties;
import diary.file.service.ImageStatusService;
import diary.file.service.filetransactionalservice.FileTransactionalService;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static diary.file.consts.FileMqProtocolConst.EVENT_ID_PREFIX;
import static diary.file.consts.FileMqProtocolConst.IMAGE_UPLOADED_EVENT_TYPE;
import static diary.file.consts.FileMqProtocolConst.IMAGE_UPLOADED_SCHEMA_VERSION;
import static diary.file.enums.FileMqOutboxStatusEnum.NEW;
import static diary.file.utils.CanonicalFormUtil.canonicalUploadedEventForm;
import static diary.utils.common.MyUtils.writeJson;
import static diary.utils.outbox.OutboxUtil.calculateRetryDelaySeconds;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileTransactionalServiceImpl implements FileTransactionalService {
    private final ImageStatusService imageStatusService;
    private final FileMqProperties fileMqProperties;
    private final FileMqOutboxMapper fileMqOutboxMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordImageUploaded(Long imageId, String objectKey, String originalName, List<String> failedFiles) {
        /*看图片状态是否为uploaded，如果是的话，开始构建outbox消息进行后续步骤*/
        ImageTransitionResultEnum transitionResultEnum = imageStatusService.markUploaded(imageId);

        int insertCnt = 0;

        long primaryKey = MyUtils.getPrimaryKey();
        if (transitionResultEnum == ImageTransitionResultEnum.APPLIED) {
            String eventId = EVENT_ID_PREFIX + primaryKey;
            LocalDateTime now = LocalDateTime.now();
            long occurredTime = now.atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            ImageUploadedEvent uploadedEvent = ImageUploadedEvent.builder()
                    .eventId(eventId)
                    .schemaVersion(IMAGE_UPLOADED_SCHEMA_VERSION)
                    .eventType(IMAGE_UPLOADED_EVENT_TYPE)
                    .imageId(imageId)
                    .objectKey(objectKey)
                    .originalName(originalName)
                    .occurredTime(occurredTime)
                    .build();

            FileMqOutboxPO fileMqOutboxPO = FileMqOutboxPO.builder()
                    .id(primaryKey)                                                                                 // 主键：新增时通常为 null，由数据库自增或雪花算法生成
                    .eventId(eventId)                                                                               // 业务事件唯一 ID，重发时保持不变
                    .imageId(imageId)                                                                               // 关联的图片 ID
                    .eventType(IMAGE_UPLOADED_EVENT_TYPE)                                                           // 业务事件类型，按实际业务填写
                    .topic(fileMqProperties.getEventTopic())                                                        // RocketMQ Topic，按实际配置填写
                    .tag(fileMqProperties.getImageUploadedTag())                                                    // RocketMQ Tag，按实际业务填写
                    .messageKey(eventId)                                                                            // 消息检索 Key，通常用业务主键，便于追踪
                    .payload(writeJson(canonicalUploadedEventForm(uploadedEvent), "图片上传消息序列化失败"))    // 已序列化的事件 JSON
                    .schemaVersion(IMAGE_UPLOADED_SCHEMA_VERSION)                                                   // 消息协议版本
                    .status(NEW.name())                                                                             // 初始状态为 NEW
                    .retryCount(0)                                                                                  // 额外发送重试次数，初始为 0
                    .maxRetries(fileMqProperties.getOutboxMaxRetries())                                             // 允许的最大额外发送重试次数，按业务配置
                    .nextRetryTime(now)                                                                             // 首次发送时间，即当前时间；也可设为立即可发送的时间
                    .brokerMessageId(null)                                                                          // 尚未发送，Broker 消息 ID 为空
                    .lastError(null)                                                                                // 尚未失败，错误信息为空
                    .sentTime(null)                                                                                 // 尚未确认发送，发送时间为空
                    .createTime(now)                                                                                // 记录创建时间
                    .updateTime(now)                                                                                // 记录更新时间
                    .versionId(0)                                                                                   // 乐观锁版本号，初始为 0
                    .build();

            // 插入file_mq_outbox表
            insertCnt = fileMqOutboxMapper.insert(fileMqOutboxPO);
        }

        if (transitionResultEnum == ImageTransitionResultEnum.ALREADY_TARGET) {
            log.warn("其他实例已经迁移到目标状态,imageId = {}", imageId);
            return;
        }

        if (transitionResultEnum == ImageTransitionResultEnum.REJECTED || insertCnt == 0) {
            log.warn("OSS成功，但状态拒绝迁移或outbox数据插入失败，imageId={}", imageId);
            failedFiles.add(originalName + ": OSS成功但状态拒绝迁移或outbox数据插入失败");
            throw new RuntimeException("OSS成功，但状态拒绝迁移或outbox数据插入失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverTimedOutForRetry(FileMqOutboxPO fileMqOutboxPO) {
        Long outboxPOId = fileMqOutboxPO.getId();
        Integer expectedVersionId = fileMqOutboxPO.getVersionId();
        Integer timeoutSeconds = fileMqProperties.getOutboxSendingTimeoutSeconds();
        Integer retryCount = fileMqOutboxPO.getRetryCount();
        Integer maxRetries = fileMqOutboxPO.getMaxRetries();
        int statusChangCnt = 0;
        // 先看可重试次数是否大于等于最大次数，如果是，则转为DEAD状态，否则转为RETRY_WAIT
        if (retryCount >= maxRetries) {
            // 标记为DEAD
            statusChangCnt = fileMqOutboxMapper.recoverTimedOutAsDead(outboxPOId, expectedVersionId, timeoutSeconds, "可重试次数大于等于最大次数");
        } else {
            // 标记为RETRY_WAIT
            statusChangCnt = fileMqOutboxMapper.recoverTimedOutForRetry(outboxPOId, expectedVersionId, timeoutSeconds, calculateRetryDelaySeconds(retryCount, maxRetries), "恢复可发送状态超时的记录");
        }
        if (statusChangCnt != 1) {
            throw new IllegalStateException("Outbox失败状态更新失败: " + fileMqOutboxPO.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean claimForPublishing(Long id, Integer versionId) {
        int claimedPubCnt = fileMqOutboxMapper.claimForPublishing(id, versionId);
        // 实例抢到数据并更新成功
        if (claimedPubCnt == 1) return true;
        // 实例未抢到数据
        if (claimedPubCnt == 0) return false;
        // 数据库异常，状态更新失败
        throw new IllegalStateException("数据库异常，Outbox SENDING状态更新失败: " + id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markSent(Long id, Integer versionId, String brokerMsgId) {
        int sentCnt = fileMqOutboxMapper.markSent(id, versionId, brokerMsgId);
        if (sentCnt == 1) return true;
        if (sentCnt == 0) return false;
        throw new IllegalStateException("数据库异常 Outbox SENT状态更新失败: " + id);
    }
}
