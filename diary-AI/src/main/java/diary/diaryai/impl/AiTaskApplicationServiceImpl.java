package diary.diaryai.impl;

import diary.common.exception.NullResultException;
import diary.diaryai.entity.ai.dto.AiInvokeDTO;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.entity.ai.vo.AiTaskSubmitVo;
import diary.diaryai.enums.aienum.AIEnum;
import diary.diaryai.enums.aienum.AiApplicationEnum;
import diary.diaryai.enums.aienum.AiFlagEnum;
import diary.diaryai.exceptions.AiSubmitRateLimitException;
import diary.diaryai.exceptions.IdempotencyConflictException;
import diary.diaryai.feignclient.DietClient;
import diary.diaryai.feignclient.RecipeClient;
import diary.diaryai.idempotency.AiRequestFingerprint;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.redis.AiSubmitRateLimiter;
import diary.diaryai.service.AiTaskApplicationService;
import diary.diaryai.service.AiTransactionService;
import diary.utils.commonutil.MyUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AiTaskApplicationServiceImpl implements AiTaskApplicationService {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTransactionService aiTransactionService;
    private final AiSubmitRateLimiter aiSubmitRateLimiter;
    private final AiRequestFingerprint requestFingerprint;
    private final RecipeClient recipeClient;
    private final DietClient dietClient;

    // 提交任务时，仅让task状态为Pending即可，当定时任务提取了消息并发送时，再改为queued
    @Override
    public AiTaskSubmitVo submitNutrientAnalyze(AiInvokeDTO aiInvokeDTO, Long userId) {
        validateAndNormalizeRequest(aiInvokeDTO);

        MyUtils.check().notNull(userId, "userId");
        String clientRequestId = aiInvokeDTO.getClientRequestId();
        // 不信任前端传来的数据
        // 校验clientRequestId---是否是同一次提交
        // 校验requestHash---是否是同一份数据
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
            // 营养分析目前只关联食谱和饮食；AI 的实际输入始终来自 ingredients。
            String targetTitle;
            switch (AiFlagEnum.valueOf(aiInvokeDTO.getFlag())) {
                case RECIPE -> targetTitle = recipeClient.getRecipeTitle(userId, aiInvokeDTO.getUniversalId()).getData();
                case DIET -> targetTitle = dietClient.getDietFoodName(userId, aiInvokeDTO.getUniversalId()).getData();
                default -> throw new IllegalArgumentException("营养分析不支持该目标类型");
            }

            if (targetTitle == null) {
                throw new NullResultException("未查询到关联对象");
            }

            aiTaskPO = aiTransactionService.createTaskAndOutbox(aiInvokeDTO, userId, targetTitle);
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

    // 使用CAS比较并替换思想，获取不同设备的相同数据请求下的最新数据
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
                .notNull(request.getIngredients(), "ingredients")
                .stringKeyMapNotContainsEmpty(request.getIngredients(), "ingredients")
                .notNull(request.getUniversalId(), "universalId");
        if (!AIEnum.isSupport(request.getAiType())) {
            throw new IllegalArgumentException("不支持的AI类型");
        }
        request.setClientRequestId(request.getClientRequestId().trim());
        request.setFlag(request.getFlag().trim().toUpperCase(Locale.ROOT));

        AiFlagEnum.isTrueFlag(request.getFlag());
        AiApplicationEnum.isTrueApplication(request.getAiApplication());
        AiFlagEnum flag = AiFlagEnum.valueOf(request.getFlag());
        if (AiApplicationEnum.NUTRITION_ANALYSIS.getCode() != request.getAiApplication()
                || (flag != AiFlagEnum.DIET && flag != AiFlagEnum.RECIPE)) {
            throw new IllegalArgumentException("AI用途与AI标志不匹配，请检查数据后重试！");
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
