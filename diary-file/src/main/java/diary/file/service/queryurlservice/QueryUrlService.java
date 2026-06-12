package diary.file.service.queryurlservice;

import diary.common.entity.image.vo.ImageVO;

import java.util.List;

public interface QueryUrlService {

    List<ImageVO> queryImageUrls(List<Long> imageIds);
}
