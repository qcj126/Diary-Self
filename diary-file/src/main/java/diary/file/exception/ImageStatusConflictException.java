package diary.file.exception;


import diary.common.exception.CustomException;
import diary.file.enums.ImageStatusEnum;

public class ImageStatusConflictException extends CustomException {
    public ImageStatusConflictException(Long imageId, ImageStatusEnum currentStatus, ImageStatusEnum targetStatus, ImageStatusEnum latestStatus) {
        super(
                "状态变更异常: imageId = " + imageId + ", " +
                "当前状态为：" + currentStatus.name() + ", " +
                "目标状态为：" + targetStatus.name() + ", " +
                "图片当前的最新状态为：" + latestStatus.name()
        );
    }
}
