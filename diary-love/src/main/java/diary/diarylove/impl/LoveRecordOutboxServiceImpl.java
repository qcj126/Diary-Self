package diary.diarylove.impl;

import diary.common.entity.mq.po.MqOutboxPO;
import diary.common.enums.outbox.OutboxStatusEnum;
import diary.diarylove.mapper.DiaryLoveMapper;
import diary.diarylove.service.LoveRecordOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoveRecordOutboxServiceImpl implements LoveRecordOutboxService {
    private final DiaryLoveMapper diaryLoveMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean claim(MqOutboxPO outbox) {
        int outboxUpdateCnt = diaryLoveMapper.claimOutbox(outbox.getId(), outbox.getVersionId());
        if (outboxUpdateCnt == 1) {
            outbox.setVersionId(outbox.getVersionId() + 1);
            outbox.setStatus(OutboxStatusEnum.SENDING.name());
            return true;
        }
        return false;
    }

    @Override
    public void confirmSent(MqOutboxPO sendingOutbox, String brokerMessageId) {
        // 每次状态的流转，必须更新versionId
        int outboxUpdateCnt = diaryLoveMapper.markOutboxSent(sendingOutbox.getId(), sendingOutbox.getVersionId(), brokerMessageId);
        if (outboxUpdateCnt != 1) {
            throw new IllegalStateException("Outbox SENT 更新失败: " + sendingOutbox.getId());
        }
        // 恋爱记录没有任务的状态，所以下游消费者直接接收消息并处理
    }

    @Override
    public void recordFailure(MqOutboxPO sendingOutbox, Throwable error) {

    }

    @Override
    public void recoverSendingTimeout(MqOutboxPO timedOutOutbox) {

    }
}
