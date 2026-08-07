package diary.file.service.queryservice;

import diary.common.entity.file.dto.IconDTO;
import diary.common.result.ApiResponse;

public interface IconQueryService {
    ApiResponse<?> queryIcons(IconDTO iconDTO);
}
