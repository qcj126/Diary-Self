package diary.diaryai.utils;

import diary.common.exception.NullResultException;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.exceptions.IdempotencyConflictException;
import diary.diaryai.idempotency.AiRequestFingerprint;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MyUtil {
    private final AiTaskProperties properties;
    private final AiRequestFingerprint requestFingerprint;
    private final DiaryAiMapper diaryAiMapper;

    public String getOutboxTag(Integer aiApplication) {
        switch (aiApplication) {
            case 1 -> {
                return properties.getRocketmq().getTaskNutrientTag();
            }
            case 2 -> {
                return properties.getRocketmq().getTaskSentimentTag();
            }
            default -> throw new NullResultException("不存在此AI用途");
        }
    }

    // 使用CAS比较并替换思想，获取不同设备的相同数据请求下的最新数据
    public void assertSameRequest(AiTaskPO existingTask, String requestHash, String aiFlag) {
        String storedHash = existingTask.getRequestHash();
        if (storedHash == null || storedHash.isBlank()) {
            String clientRequestId = existingTask.getClientRequestId();
            String snapshotHash = requestFingerprint.fingerprintSnapshot(existingTask.getInputSnapshot(), aiFlag, clientRequestId);
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
}
