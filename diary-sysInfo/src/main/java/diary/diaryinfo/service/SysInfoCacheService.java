package diary.diaryinfo.service;

import java.util.List;
import java.util.Optional;

public interface SysInfoCacheService {
    <T> Optional<List<T>> getList(String key, Class<T> elementType);

    void putList(String key, List<?> values);
}
