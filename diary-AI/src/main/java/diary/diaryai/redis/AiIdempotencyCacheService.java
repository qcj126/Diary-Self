package diary.diaryai.redis;

import java.util.Optional;

public interface AiIdempotencyCacheService {
    Optional<Long> get(Long userId, String clientRequestId);

    void put(Long userId, String clientRequestId, Long taskId);

    void evict(Long userId, String clientRequestId);
}
