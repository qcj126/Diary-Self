package diary.file.service.updateservice;

import diary.common.entity.file.dto.IconDTO;
import diary.common.result.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IconUpdateService {
    ApiResponse<?> updateIcon(MultipartFile file, IconDTO iconDTO);
}
