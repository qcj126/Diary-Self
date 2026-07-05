package diary.file.impl.queryurlserviceImpl;

import diary.common.entity.image.po.ImagePO;
import diary.common.entity.image.vo.ImageVO;
import diary.common.exception.ParamIllegalException;
import diary.file.mapper.ImageMapper;
import diary.file.service.queryurlservice.QueryUrlService;
import diary.utils.OSS.OssUtil;
import diary.utils.redis.RedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class QueryUrlServiceImpl implements QueryUrlService {
    @Resource
    private ImageMapper imageMapper;

    @Resource
    private OssUtil ossUtil;

    @Resource
    private RedisUtil redisUtil;

    public List<ImageVO> queryImageUrls(List<Long> imageIds) {
        if (imageIds.stream().anyMatch(Objects::isNull)) {
            throw new ParamIllegalException("imageIds 不能为空");
        }
        List<ImageVO> imageVOS = new ArrayList<>();
        // 先走缓存
        for (Long imageId : imageIds) {
            String url = redisUtil.getStringWithExpire("imageUrls:" + imageId);
            ImageVO imageVO = new ImageVO();
            if (url == null) {
                // 1. 从数据库查询 object_key
                ImagePO imagePO = imageMapper.selectImageById(imageId);

                // 2. 提取所有 object_key
                String objectKey = imagePO.getObjectKey();

                // 3. 批量生成签名 URL
                Map<Long, String> signedUrlMap = ossUtil.generateSignedUrl(objectKey, imageId);
                // 将新生成的url插入redis
                redisUtil.setStringWithExpire("imageUrls:" + imageId, signedUrlMap.get(imageId), 10);
                imageVO.setId(imageId);
                imageVO.setUrl(signedUrlMap.get(imageId));
            } else {
                imageVO.setId(imageId);
                imageVO.setUrl(url);
            }
            imageVOS.add(imageVO);
        }
        return imageVOS;
    }

    @Override
    public List<ImageVO> queryCarouselImages() {
        List<ImagePO> imagePOS = imageMapper.selectLatestTwoImagesForCarousel();
        return queryImageUrls(imagePOS.stream().map(ImagePO::getId).toList());
    }
}
