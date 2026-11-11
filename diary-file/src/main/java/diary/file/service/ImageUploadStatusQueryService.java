package diary.file.service;

import diary.file.entity.vo.ImageUploadStatusVO;

import java.util.List;

public interface ImageUploadStatusQueryService {
    List<ImageUploadStatusVO> queryStatuses(List<Long> imageIds, Long userId);
}
