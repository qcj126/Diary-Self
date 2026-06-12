package diary.file.impl.queryurlserviceImpl;

import diary.common.entity.image.po.ImagePO;
import diary.common.entity.image.vo.ImageVO;
import diary.common.exception.ParamIllegalException;
import diary.file.mapper.ImageMapper;
import diary.file.service.queryurlservice.QueryUrlService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class QueryUrlServiceImpl implements QueryUrlService {
    @Resource
    private ImageMapper imageMapper;

    public List<ImageVO> queryImageUrls(List<Long> imageIds) {
        if (imageIds.stream().anyMatch(Objects::isNull)) {
            throw new ParamIllegalException("imageIds 不能为空");
        }
        List<ImagePO> imagePOS = imageMapper.selectImagesByIds(imageIds);
        return imagePOS.stream().map(imagePO -> {
            ImageVO imageVO = new ImageVO();
            imageVO.setUrl(imagePO.getUrl());
            imageVO.setId(imagePO.getId());
            return imageVO;
        }).toList();
    }
}
