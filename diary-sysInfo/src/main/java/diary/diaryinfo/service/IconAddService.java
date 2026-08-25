package diary.diaryinfo.service;

import diary.common.entity.file.dto.IconDTO;
import diary.common.result.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IconAddService {
    ApiResponse<?> addIcon(MultipartFile file, IconDTO iconDTO);
}
