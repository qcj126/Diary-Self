package diary.file.impl.queryserviceImpl;

import diary.file.entity.po.ImagePO;
import diary.file.entity.vo.ImageVO;
import diary.file.enums.ImageStatusEnum;
import diary.file.mapper.ImageMapper;
import diary.file.service.queryurlservice.QueryUrlService;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class QueryUrlServiceImpl implements QueryUrlService {
    private final ImageMapper imageMapper;

    public List<ImageVO> queryImageUrls(List<Long> imageIds, Long userId) {
        MyUtils.check().listNotContainsEmpty(imageIds, "图片ID列表").notNull(userId, "userId");
        List<ImageVO> imageVOS = new ArrayList<>();
        for (Long imageId : imageIds) {
            ImagePO imagePO = imageMapper.selectForUploadByIdAndUserId(imageId, userId);
            if (imagePO == null || !Objects.equals(imagePO.getStatus(), ImageStatusEnum.READY.getCode())) continue;
            ImageVO imageVO = new ImageVO();
            imageVO.setId(imageId);
            imageVO.setUrl("/file/originals/" + imageId);
            imageVOS.add(imageVO);
        }
        return imageVOS;
    }

    @Override
    public List<ImageVO> queryCarouselImages(Long userId) {
        List<ImagePO> imagePOS = imageMapper.selectLatestTwoImagesForCarousel();
        return queryImageUrls(imagePOS.stream().map(ImagePO::getId).toList(), userId);
    }
}
