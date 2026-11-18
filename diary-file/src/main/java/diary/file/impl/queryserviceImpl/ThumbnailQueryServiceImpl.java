package diary.file.impl.queryserviceImpl;

import diary.file.entity.model.OssObjectResource;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ThumbnailPO;
import diary.file.entity.vo.ImageVO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.ImageTypeEnum;
import diary.file.enums.ThumbnailStatusEnum;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ThumbnailMapper;
import diary.file.service.oss.OssStorageService;
import diary.file.service.queryurlservice.ThumbnailQueryService;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ThumbnailQueryServiceImpl implements ThumbnailQueryService {
    private final ImageMapper imageMapper;
    private final ThumbnailMapper thumbnailMapper;
    private final OssStorageService ossStorageService;

    @Override
    public List<ImageVO> queryThumbnailUrls(List<Long> imageIds, Long userId) {
        MyUtils.check()
                .notNull(userId, "userId")
                .listNotContainsEmpty(imageIds, "图片ID列表");
        List<Long> distinctImageIds = imageIds.stream().distinct().toList();
        List<Integer> thumbnailTypes = Arrays.stream(ImageTypeEnum.values())
                .filter(ImageTypeEnum::isThumbnailRequired)
                .map(ImageTypeEnum::getCode)
                .toList();
        List<ThumbnailPO> thumbnails = thumbnailMapper.selectReadyByImageIdsAndUserId(
                distinctImageIds,
                userId,
                thumbnailTypes,
                ImageStatusEnum.READY.getCode(),
                ThumbnailStatusEnum.READY.getCode()
        );
        List<ImageVO> result = new ArrayList<>(thumbnails.size());
        for (ThumbnailPO thumbnail : thumbnails) {
            ImageVO imageVO = new ImageVO();
            imageVO.setId(thumbnail.getImageId());
            imageVO.setUrl("/file/thumbnails/" + thumbnail.getImageId());
            result.add(imageVO);
        }
        return result;
    }

    @Override
    public OssObjectResource openThumbnail(Long imageId, Long userId) {
        MyUtils.check().notNull(imageId, "imageId").notNull(userId, "userId");
        ThumbnailPO thumbnail = findReadyThumbnail(imageId, userId);
        return thumbnail == null ? null : ossStorageService.openObject(thumbnail.getObjectKey());
    }

    private ThumbnailPO findReadyThumbnail(Long imageId, Long userId) {
        ImagePO image = imageMapper.selectForUploadByIdAndUserId(imageId, userId);
        if (image == null
                || !Objects.equals(image.getStatus(), ImageStatusEnum.READY.getCode())
                || !ImageTypeEnum.fromCode(image.getType()).isThumbnailRequired()) {
            return null;
        }
        ThumbnailPO thumbnail = thumbnailMapper.selectByImageIdAndUserId(imageId, userId);
        if (thumbnail == null
                || !Objects.equals(thumbnail.getStatus(), ThumbnailStatusEnum.READY.getCode())
                || !Objects.equals(thumbnail.getDeleted(), 0)
                || thumbnail.getObjectKey() == null
                || thumbnail.getObjectKey().isBlank()) {
            return null;
        }
        return thumbnail;
    }
}
