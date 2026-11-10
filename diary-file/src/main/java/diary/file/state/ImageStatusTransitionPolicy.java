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

            case UPLOADING -> targetStatus == UPLOADED || targetStatus == RETRY_WAIT || targetStatus == UPLOAD_FAILED;

            case RETRY_WAIT -> targetStatus == UPLOADING || targetStatus == UPLOAD_FAILED;

            case UPLOADED -> targetStatus == READY;

            case READY, UPLOAD_FAILED -> false;
        };
    }
}
