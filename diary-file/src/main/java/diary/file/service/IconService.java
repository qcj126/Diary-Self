package diary.file.service;

import diary.common.entity.file.dto.IconDTO;
import diary.common.result.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IconService {
    ApiResponse<?> addIcon(MultipartFile file, IconDTO iconDTO);

    ApiResponse<?> queryIcons(IconDTO iconDTO);

    ApiResponse<?> updateIcon(MultipartFile file, IconDTO iconDTO);

    ApiResponse<?> deleteIcon(IconDTO iconDTO);
}
