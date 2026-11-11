package diary.file.impl;

import diary.file.entity.convert.PoConvertToVo;
import diary.file.entity.po.ImagePO;
import diary.file.entity.vo.ImageUploadStatusVO;
import diary.file.mapper.ImageMapper;
import diary.file.service.ImageUploadStatusQueryService;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadStatusQueryServiceImpl implements ImageUploadStatusQueryService {
    private final ImageMapper imageMapper;
    @Override
    public List<ImageUploadStatusVO> queryStatuses(List<Long> imageIds, Long userId) {
        MyUtils.check()
                .notNull(imageIds, "图片ID列表")
                .listNotEmpty(imageIds, "图片ID列表")
                .listNotContainsEmpty(imageIds, "图片ID列表")
                .notNull(userId, "用户id");
        // 对imageId进行去重
        List<Long> list = imageIds.stream().distinct().toList();

        // 查询图片状态
        List<ImagePO> imagePOS = imageMapper.selectImageStatusesByIdsAndUserId(list, userId);

        // 将集合转为Map，方便后续进行数据索引
        Map<Long, ImagePO> imagePOMap = imagePOS.stream().collect(Collectors.toMap(
                ImagePO::getId,
                Function.identity(),
                (first, ignore) -> first
        ));

        List<ImageUploadStatusVO> uploadStatusVOS = new ArrayList<>();
        for (Long imageId : list) {
            ImagePO imagePO = imagePOMap.get(imageId);
            if (imagePO == null) continue;
            ImageUploadStatusVO imageUploadStatusVO = PoConvertToVo.convertToImageUploadStatusVO(imagePO);
            uploadStatusVOS.add(imageUploadStatusVO);
        }

        return uploadStatusVOS;
    }
}
