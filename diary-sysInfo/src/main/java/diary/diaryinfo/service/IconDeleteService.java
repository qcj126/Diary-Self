package diary.diaryinfo.service;

import diary.common.entity.file.dto.IconDTO;
import diary.common.result.ApiResponse;

public interface IconDeleteService {
    ApiResponse<?> deleteIcon(IconDTO iconDTO);
}
