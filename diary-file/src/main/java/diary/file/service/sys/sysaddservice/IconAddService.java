package diary.file.service.sys.sysaddservice;

import diary.common.result.ApiResponse;
import diary.file.entity.dto.IconDTO;
import org.springframework.web.multipart.MultipartFile;

public interface IconAddService {
    ApiResponse<?> addIcon(MultipartFile file, IconDTO iconDTO);
}
