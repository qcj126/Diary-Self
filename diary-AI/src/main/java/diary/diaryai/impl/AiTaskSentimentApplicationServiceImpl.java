package diary.diaryai.impl;

import diary.common.entity.feign.LoveRecordAiSnapshotDTO;
import diary.common.exception.NullResultException;
import diary.diaryai.entity.ai.dto.LoveRecordSentimentSubmitDTO;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.entity.ai.vo.AiTaskSubmitVo;
import diary.diaryai.enums.aienum.AIEnum;
import diary.diaryai.enums.aienum.AiFlagEnum;
import diary.diaryai.exceptions.AiSubmitRateLimitException;
import diary.diaryai.feignclient.LoveRecordClient;
import diary.diaryai.idempotency.AiRequestFingerprint;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.redis.AiSubmitRateLimiter;
import diary.diaryai.service.AiTaskSentimentApplicationService;
import diary.diaryai.service.AiTransactionService;
import diary.diaryai.utils.MyUtil;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskSentimentApplicationServiceImpl implements AiTaskSentimentApplicationService {
    private final AiTransactionService aiTransactionService;
    private final AiRequestFingerprint requestFingerprint;
    private final AiSubmitRateLimiter aiSubmitRateLimiter;
    private final LoveRecordClient loveRecordClient;
    private final DiaryAiMapper diaryAiMapper;
    private final MyUtil myUtil;

    @Override
    public AiTaskSubmitVo submitLoveRecordSentimentAnalyze(LoveRecordSentimentSubmitDTO loveRecordSentimentSubmitDTO, Long userId) {
        validateAndNormalizeRequest(loveRecordSentimentSubmitDTO);
        MyUtils.check().notNull(userId, "userId");

        String requestHash = requestFingerprint.fingerprint(loveRecordSentimentSubmitDTO, AiFlagEnum.LOVE_RECORD.name());
        String clientRequestId = loveRecordSentimentSubmitDTO.getClientRequestId();
        AiTaskPO existingTask = diaryAiMapper.selectByUserIdAndClientRequestId(userId, clientRequestId);
        if (existingTask != null) {
            myUtil.assertSameRequest(existingTask, requestHash, AiFlagEnum.LOVE_RECORD.name());
            return toSubmitVo(existingTask, "该请求已提交");
        }

        boolean allow = aiSubmitRateLimiter.allow(userId);
        if (!allow) throw new AiSubmitRateLimitException("请求次数已达上限");

        AiTaskPO aiTaskPO;
        try {
            // 异步创建aiTask和outbox，绑定同一最小事务，确保数据的一致性
            // 营养分析目前只关联食谱和饮食；AI 的实际输入始终来自 ingredients。
            LoveRecordAiSnapshotDTO loveRecordAiSnapshotDTO = loveRecordClient.getLoveRecordAiSnapshot(userId, loveRecordSentimentSubmitDTO.getLoveRecordId()).getData();

            if (loveRecordAiSnapshotDTO == null) {
                throw new NullResultException("未查询到关联对象");
            }

            aiTaskPO = aiTransactionService.createLoveRecordSentimentTaskAndOutbox(loveRecordAiSnapshotDTO, userId, loveRecordSentimentSubmitDTO);
            if (aiTaskPO == null) {
                throw new IllegalStateException("AI任务创建失败");
            }
        } catch (DuplicateKeyException duplicateKeyException) {
            // aiTask表有userId和clientRequestId联合唯一索引
            // 当并发请求同时插入时，后插入的请求会抛出 DuplicateKeyException
            AiTaskPO concurrentTask = diaryAiMapper.selectByUserIdAndClientRequestId(userId, clientRequestId);
            if (concurrentTask == null) {
                // 两个不同请求在极短时间到达，taskId生成相同，但是userId或clientRequestId不同，那么就查不到数据
                // 此时就不是联合唯一索引冲突，而是主键冲突了
                throw duplicateKeyException;
            }
            myUtil.assertSameRequest(concurrentTask, requestHash, AiFlagEnum.LOVE_RECORD.name());
            return toSubmitVo(concurrentTask, "该请求已提交");
        }
        return toSubmitVo(aiTaskPO, "AI分析任务已受理");
    }

    private void validateAndNormalizeRequest(LoveRecordSentimentSubmitDTO request) {
        MyUtils.check()
                .notNull(request, "LoveRecordSentimentSubmitDTO")
                .notEmpty(request.getClientRequestId(), "clientRequestId")
                .notNull(request.getAiType(), "aiType")
                .notNull(request.getLoveRecordId(), "loveRecordId");
        if (!AIEnum.isSupport(request.getAiType())) {
            throw new IllegalArgumentException("不支持的AI类型");
        }
    }

    private AiTaskSubmitVo toSubmitVo(AiTaskPO task, String message) {
        return AiTaskSubmitVo.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .message(message)
                .build();
    }
}
