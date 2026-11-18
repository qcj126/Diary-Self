package diary.file.state;

import diary.file.enums.ImageStatusEnum;
import org.springframework.stereotype.Component;

import static diary.file.enums.ImageStatusEnum.READY;
import static diary.file.enums.ImageStatusEnum.RETRY_WAIT;
import static diary.file.enums.ImageStatusEnum.UPLOADED;
import static diary.file.enums.ImageStatusEnum.UPLOADING;
import static diary.file.enums.ImageStatusEnum.UPLOAD_FAILED;

@Component
public class ImageStatusTransitionPolicy {
    public boolean canTransition(ImageStatusEnum currentStatus, ImageStatusEnum targetStatus) {
        if (currentStatus == null || targetStatus == null) return false;

        return switch (currentStatus) {
            case PENDING_UPLOAD -> targetStatus == UPLOADING || targetStatus == UPLOAD_FAILED;

            /* 不需要缩略图的类型在整文件校验成功后可以直接进入READY。 */
            case UPLOADING -> targetStatus == UPLOADED || targetStatus == READY
                    || targetStatus == RETRY_WAIT || targetStatus == UPLOAD_FAILED;

            case RETRY_WAIT -> targetStatus == UPLOADING || targetStatus == UPLOAD_FAILED;

            /*原图已落OSS但缩略图永久无法生成时，必须收敛为失败终态。*/
            case UPLOADED -> targetStatus == READY || targetStatus == UPLOAD_FAILED;

            case READY, UPLOAD_FAILED -> false;
        };
    }
}
