package diary.file.service.sys.sysdeleteservice;

import diary.common.result.ApiResponse;
import diary.file.entity.dto.IconDTO;

public interface IconDeleteService {
    ApiResponse<?> deleteIcon(IconDTO iconDTO);
}
