package diary.diaryai.redis;

import diary.common.entity.ai.vo.AiTaskStatusVo;

import java.util.Optional;

public interface AiTaskCacheService {
    Optional<AiTaskStatusVo> get(Long taskId, Long userId);

    void put(AiTaskStatusVo value, Long userId);

    void evict(Long taskId);
}
