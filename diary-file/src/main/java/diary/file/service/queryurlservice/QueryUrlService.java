package diary.file.service.queryurlservice;

import diary.file.entity.vo.ImageVO;

import java.util.List;

public interface QueryUrlService {

    List<ImageVO> queryImageUrls(List<Long> imageIds, Long userId);

    List<ImageVO> queryCarouselImages(Long userId);
}
