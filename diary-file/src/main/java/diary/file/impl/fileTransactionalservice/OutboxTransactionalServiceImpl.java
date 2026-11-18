package diary.file.impl.fileTransactionalservice;

import diary.file.entity.mq.po.FileMqOutboxPO;
import diary.file.mapper.FileMqOutboxMapper;
import diary.file.property.FileMqProperties;
import diary.file.service.filetransactionalservice.OutboxTransactionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static diary.utils.outbox.OutboxUtil.calculateRetryDelaySeconds;

@Service
@RequiredArgsConstructor
public class OutboxTransactionalServiceImpl implements OutboxTransactionalService {
    private final FileMqProperties fileMqProperties;
    private final FileMqOutboxMapper fileMqOutboxMapper;

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
