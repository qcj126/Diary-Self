package diary.file.service.sys.sysupdateservice;

import diary.common.result.ApiResponse;
import diary.file.entity.dto.IconDTO;
import org.springframework.web.multipart.MultipartFile;

public interface IconUpdateService {
    ApiResponse<?> updateIcon(MultipartFile file, IconDTO iconDTO);
}
