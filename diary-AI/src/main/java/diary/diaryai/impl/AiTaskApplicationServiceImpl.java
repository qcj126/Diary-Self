package diary.diaryai.impl;

import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.entity.ai.vo.AiTaskSubmitVo;
import diary.common.enums.aienum.AIEnum;
import diary.common.enums.aienum.AiApplicationEnum;
import diary.common.enums.aienum.AiFlagEnum;
import diary.common.exception.AiSubmitRateLimitException;
import diary.common.exception.IdempotencyConflictException;
import diary.diaryai.idempotency.AiRequestFingerprint;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.redis.AiSubmitRateLimiter;
import diary.diaryai.service.AiTaskApplicationService;
import diary.diaryai.service.AiTaskCommandService;
import diary.utils.commonutil.MyUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiTaskApplicationServiceImpl implements AiTaskApplicationService {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskCommandService aiTaskCommandService;
    private final AiSubmitRateLimiter aiSubmitRateLimiter;
    private final AiRequestFingerprint requestFingerprint;

    // 提交任务时，仅让task状态为Pending即可，当定时任务提取了消息并发送时，再改为queued
    @Override
    public AiTaskSubmitVo submitTask(AiInvokeDTO aiInvokeDTO, Long userId) {
        validateAndNormalizeRequest(aiInvokeDTO);

        MyUtils.check().notNull(userId, "userId");
        String clientRequestId = aiInvokeDTO.getClientRequestId();
        String requestHash = requestFingerprint.fingerprint(aiInvokeDTO);

        // MySQL 联合唯一索引是幂等性的唯一权威，避免再维护一套 Redis 幂等缓存。
        AiTaskPO existingTask = diaryAiMapper.selectByUserIdAndClientRequestId(userId, clientRequestId);
        if (existingTask != null) {
            assertSameRequest(existingTask, requestHash);
            return toSubmitVo(existingTask, "该请求已提交");
        }

        // 真正创建任务和outbox前，检查请求次数是否已达上限
        boolean allow = aiSubmitRateLimiter.allow(userId);
        if (!allow) {
            throw new AiSubmitRateLimitException("请求次数已达上限");
        }

        AiTaskPO aiTaskPO;
        try {
            // 异步创建aiTask和outbox，绑定同一最小事务，确保数据的一致性
            aiTaskPO = aiTaskCommandService.createTaskAndOutbox(aiInvokeDTO, userId);
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
            assertSameRequest(concurrentTask, requestHash);
            return toSubmitVo(concurrentTask, "该请求已提交");
        }
        return toSubmitVo(aiTaskPO, "AI分析任务已受理");
    }

    private void assertSameRequest(AiTaskPO existingTask, String requestHash) {
        String storedHash = existingTask.getRequestHash();
        if (storedHash == null || storedHash.isBlank()) {
            String snapshotHash = requestFingerprint.fingerprintSnapshot(existingTask.getInputSnapshot());
            int backfilled = diaryAiMapper.updateRequestHashIfNull(existingTask.getId(), snapshotHash);
            if (backfilled == 1) {
                storedHash = snapshotHash;
            } else {
                // 存量任务首次幂等重放可能并发回填；未抢到 CAS 的请求必须重读胜出值。
                AiTaskPO refreshed = diaryAiMapper.selectAiTaskByTaskId(existingTask.getId());
                storedHash = refreshed == null ? null : refreshed.getRequestHash();
            }
        }
        if (storedHash == null || !storedHash.equals(requestHash)) {
            throw new IdempotencyConflictException(
                    "clientRequestId已用于不同的AI请求: " + existingTask.getClientRequestId());
        }
    }

    private void validateAndNormalizeRequest(AiInvokeDTO request) {
        MyUtils.check()
                .notNull(request, "aiInvokeDTO")
                .notEmpty(request.getClientRequestId(), "clientRequestId")
                .notNull(request.getAiType(), "aiType")
                .notNull(request.getAiApplication(), "aiApplication")
                .notEmpty(request.getFlag(), "flag")
                .notNull(request.getMaterials(), "materials")
                .stringKeyMapNotContainsEmpty(request.getMaterials(), "materials")
                .notNull(request.getUniversalId(), "universalId");
        if (!AIEnum.isSupport(request.getAiType())) {
            throw new IllegalArgumentException("不支持的AI类型");
        }
        AiFlagEnum.isTrueFlag(request.getFlag());
        AiApplicationEnum.isTrueApplication(request.getAiApplication());
        request.setClientRequestId(request.getClientRequestId().trim());
        request.setFlag(request.getFlag().trim().toUpperCase());
    }

    private AiTaskSubmitVo toSubmitVo(AiTaskPO task, String message) {
        return AiTaskSubmitVo.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .message(message)
                .build();
    }
}
