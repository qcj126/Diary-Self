package diary.file.service.uploadservice;

import diary.file.entity.dto.ImageUploadInitReqDto;
import diary.file.entity.vo.ImageUploadConfigVO;
import diary.file.entity.vo.ImageUploadInitVO;

public interface ImageUploadInitService {
    ImageUploadInitVO initialize(
            ImageUploadInitReqDto request,
            Long userId
    );

    ImageUploadConfigVO getUploadConfig();
}
