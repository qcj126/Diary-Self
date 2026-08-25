package diary.diaryinfo.service;

import diary.common.entity.file.dto.IconDTO;
import diary.common.result.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface IconQueryService {
    ApiResponse<?> queryIcons(IconDTO iconDTO);

    ResponseEntity<byte[]> getIcon(String fileName);
}
