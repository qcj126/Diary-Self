package diary.file.impl;

import diary.common.exception.NullResultException;
import diary.file.entity.po.ImagePO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.ImageTransitionResultEnum;
import diary.file.exception.ImageStatusConflictException;
import diary.file.mapper.ImageMapper;
import diary.file.service.ImageStatusService;
import diary.file.state.ImageStatusTransitionPolicy;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static diary.file.enums.ImageStatusEnum.fromCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStatusServiceImpl implements ImageStatusService {
    private final ImageStatusTransitionPolicy policy;
    private final ImageMapper imageMapper;

    @Override
    public ImageTransitionResultEnum markUploading(Long imageId) {
        return transition(imageId, ImageStatusEnum.UPLOADING);
    }

    @Override
    public ImageTransitionResultEnum markUploaded(Long imageId) {
        return transition(imageId, ImageStatusEnum.UPLOADED);
    }

    @Override
    public ImageTransitionResultEnum markReady(Long imageId) {
        return transition(imageId, ImageStatusEnum.READY);
    }

    @Override
    public ImageTransitionResultEnum markRetryWait(Long imageId) {
        return transition(imageId, ImageStatusEnum.RETRY_WAIT);
    }

    @Override
    public ImageTransitionResultEnum markUploadFailed(Long imageId) {
        return transition(imageId, ImageStatusEnum.UPLOAD_FAILED);
    }

    /*
    * 此处的上传场景考虑：
    *   初始状态：PENDING_UPLOAD
    *
    *   实例 A 查询：PENDING_UPLOAD
    *   实例 B 查询：PENDING_UPLOAD
    *
    *   实例 A 执行 CAS：
    *   WHERE status = PENDING_UPLOAD
    *   更新为 UPLOADING
    *   返回 1
    *
    *   实例 B 执行 CAS：
    *   WHERE status = PENDING_UPLOAD
    *   但数据库已经是 UPLOADING
    *   返回 0
    *
    * */
    private ImageTransitionResultEnum transition(Long imageId, ImageStatusEnum targetStatus) {
        // 查询图片当前状态
        MyUtils.check().notNull(imageId, "imageId");
        ImagePO imagePO = imageMapper.selectImageStatusById(imageId);
        if (imagePO == null) throw new NullResultException("imageId：" + imageId + "查询图片信息为空");
        // 将图片状态转为枚举
        ImageStatusEnum currentStatus = fromCode(imagePO.getStatus());
        if (currentStatus == targetStatus) return ImageTransitionResultEnum.ALREADY_TARGET;
        if (!policy.canTransition(currentStatus, targetStatus)) {
            return ImageTransitionResultEnum.REJECTED;
        }
        // 更新数据库，需要考虑并发下CAS的问题
        int markedCnt = imageMapper.updateStatusIfCurrent(imageId, currentStatus.getCode(), targetStatus.getCode());
        if (markedCnt == 1) return ImageTransitionResultEnum.APPLIED;
        // 再次查询最新状态
        imagePO = imageMapper.selectImageStatusById(imageId);
        if (imagePO == null) {
            throw new NullResultException("当前图片已被删除, imageId = " + imageId);
        }
        ImageStatusEnum latestStatus = fromCode(imagePO.getStatus());
        if (latestStatus != targetStatus) {
            // 另一实例让此图片处于了另一种目标状态，与当前目标状态冲突，需要抛出冲突异常
            throw new ImageStatusConflictException(imageId, currentStatus, targetStatus, latestStatus);
        } else {
            return ImageTransitionResultEnum.ALREADY_TARGET;
        }
    }
}
