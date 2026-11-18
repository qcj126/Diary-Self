package diary.file.service.queryurlservice;

import diary.file.entity.model.OssObjectResource;
import diary.file.entity.vo.ImageVO;

import java.util.List;

/** 查询缩略图访问地址并按当前用户读取缩略图对象。 */
public interface ThumbnailQueryService {
    List<ImageVO> queryThumbnailUrls(List<Long> imageIds, Long userId);

    OssObjectResource openThumbnail(Long imageId, Long userId);
}
