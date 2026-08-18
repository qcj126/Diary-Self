package diary.diaryai.redis;

import diary.common.entity.ai.vo.AiTaskStatusVo;

import java.util.Optional;

public interface AiTaskCacheService {
    Optional<AiTaskStatusVo> get(Long taskId);

    void put(AiTaskStatusVo value);

    void evict(Long taskId);
}
