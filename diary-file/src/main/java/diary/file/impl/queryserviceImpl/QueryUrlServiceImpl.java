package diary.file.impl.queryserviceImpl;

import diary.file.entity.po.ImagePO;
import diary.file.entity.vo.ImageVO;
import diary.file.mapper.ImageMapper;
import diary.file.service.ImageFileService;
import diary.file.service.queryurlservice.QueryUrlService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class QueryUrlServiceImpl implements QueryUrlService {
    @Resource
    private ImageMapper imageMapper;

    @Resource
    private ImageFileService imageFileService;

    public List<ImageVO> queryImageUrls(List<Long> imageIds) {
        MyUtils.check().listNotContainsEmpty(imageIds, "图片ID列表");
        List<ImageVO> imageVOS = new ArrayList<>();
        for (Long imageId : imageIds) {
            ImageVO imageVO = new ImageVO();
            ImagePO imagePO = imageMapper.selectImageById(imageId);
            if (imagePO == null) continue;
            if (imageFileService.getOrRestore(imagePO) == null) {
                log.warn("图片不可用，本地与 OSS 均未找到: id={}, originalName={}", imageId, imagePO.getOriginalName());
                continue;
            }
            imageVO.setId(imageId);
            imageVO.setUrl("/file/images/" + UriUtils.encodePathSegment(imagePO.getOriginalName(), StandardCharsets.UTF_8));
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
