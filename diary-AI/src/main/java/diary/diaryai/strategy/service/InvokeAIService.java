package diary.diaryai.strategy.service;

/**
 * 调用AI的顶级接口
 */
public interface InvokeAIService {
    /*
     * 以前策略落库只携带 taskId，无法证明当前线程仍拥有 RUNNING 任务；租约过期后，旧 Worker
     * 可能覆盖新 Worker 的结果。现在把抢占时生成的 workerId 和 versionId 沿原有调用层级向下传递，
     * 最终由结果事务执行带所有权条件的 SUCCESS 更新。
     */
    void getAiResultAndSave(Object data, Long taskId, Long userId, String workerId, Integer versionId);
    Integer getCode();
}
