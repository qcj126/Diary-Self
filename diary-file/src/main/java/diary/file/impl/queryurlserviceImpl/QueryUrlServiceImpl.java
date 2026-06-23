package diary.file.impl.queryurlserviceImpl;

import diary.common.entity.image.po.ImagePO;
import diary.common.entity.image.vo.ImageVO;
import diary.common.exception.ParamIllegalException;
import diary.file.mapper.ImageMapper;
import diary.file.service.queryurlservice.QueryUrlService;
import diary.utils.OSS.OssUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public List<ImageVO> queryImageUrls(List<Long> imageIds) {
        if (imageIds.stream().anyMatch(Objects::isNull)) {
            throw new ParamIllegalException("imageIds 不能为空");
        }
        // 1. 从数据库查询 object_key
        List<ImagePO> imagePOS = imageMapper.selectImagesByIds(imageIds);
        
        // 2. 提取所有 object_key
        List<String> objectKeys = imagePOS.stream()
                .map(ImagePO::getObjectKey)
                .toList();
        
        // 3. 批量生成签名 URL
        Map<String, String> signedUrlMap = ossUtil.batchGenerateSignedUrls(objectKeys);
        
        // 4. 构建返回结果
        return imagePOS.stream().map(imagePO -> {
            ImageVO imageVO = new ImageVO();
            imageVO.setId(imagePO.getId());
            // 根据 object_key 获取对应的签名 URL
            String signedUrl = signedUrlMap.get(imagePO.getObjectKey());
            imageVO.setUrl(signedUrl);
            return imageVO;
        }).toList();
    }
}
