package diary.file.mapper;

import diary.file.entity.mq.po.FileMqOutboxPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileMqOutboxMapper {
    /*新增一条outbox数据*/
    int insert(FileMqOutboxPO outbox);
    /*查询一条outbox消息*/
    FileMqOutboxPO selectById(
            @Param("id") Long id
    );
    /*查询一批可投递到mq的outbox消息，让下游的消费者进行消费*/
    List<FileMqOutboxPO> selectPublishableOutboxBatch(
            @Param("limit") int limit
    );
    /*流转状态为可发送状态SENDING*/
    int claimForPublishing(
            @Param("id") Long id,
            @Param("expectedVersionId") Integer expectedVersionId
    );
    /*确认mq已收到消息*/
    int markSent(
            @Param("id") Long id,
            @Param("expectedVersionId") Integer expectedVersionId,
            @Param("brokerMessageId") String brokerMessageId
    );
    /*若outbox消息发送到mq失败，进入RETRY_WAIT状态*/
    int markRetryWait(
            @Param("id") Long id,
            @Param("expectedVersionId") Integer expectedVersionId,
            @Param("nextRetryTime") long nextRetryTime,
            @Param("lastError") String lastError
    );
    /*如果可重试次数耗尽，或某些特定异常发生时，进入DEAD状态*/
    int markDead(
            @Param("id") Long id,
            @Param("expectedVersionId") Integer expectedVersionId,
            @Param("lastError") String lastError
    );
    /*查找超时的outbox记录*/
    List<FileMqOutboxPO> selectTimedOutSendingBatch(
            @Param("timeoutSeconds") long timeoutSeconds,
            @Param("limit") int limit
    );
    /*将超时的outbox记录流转为RETRY_WAIT*/
    int recoverTimedOutForRetry (
            @Param("id") Long id,
            @Param("expectedVersionId") Integer expectedVersionId,
            @Param("timeoutSeconds") long timeoutSeconds,
            @Param("retryDelaySeconds") long retryDelaySeconds,
            @Param("lastError") String lastError
    );
    /*将超过最大可重试次数的超时记录流转为DEAD*/
    int recoverTimedOutAsDead(
            @Param("id") Long id,
            @Param("expectedVersionId") Integer expectedVersionId,
            @Param("timeoutSeconds") long timeoutSeconds,
            @Param("lastError") String lastError
    );
    /*清理已经发送的outbox记录*/
    int cleanSentOutboxData(
            @Param("sentRetentionDays") int sentRetentionDays,
            @Param("cleanBatchSize") int cleanBatchSize
    );
}
