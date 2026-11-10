package diary.file.service;

import diary.file.enums.ImageTransitionResultEnum;

public interface ImageStatusService {
    ImageTransitionResultEnum markUploading(Long imageId);
    ImageTransitionResultEnum markUploaded(Long imageId);
    ImageTransitionResultEnum markReady(Long imageId);
    ImageTransitionResultEnum markRetryWait(Long imageId);
    ImageTransitionResultEnum markUploadFailed(Long imageId);
}
