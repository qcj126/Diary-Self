package diary.file.service.sys.sysqueryservice;

import diary.common.result.ApiResponse;
import diary.file.entity.dto.IconDTO;
import org.springframework.http.ResponseEntity;

public interface IconQueryService {
    ApiResponse<?> queryIcons(IconDTO iconDTO);

    ResponseEntity<byte[]> getIcon(String fileName);
}
