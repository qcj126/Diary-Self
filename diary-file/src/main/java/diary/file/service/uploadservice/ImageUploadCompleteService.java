package diary.file.service.uploadservice;

import diary.file.entity.dto.ImageUploadCompleteReqDto;
import diary.file.entity.vo.ImageUploadCompleteVO;

public interface ImageUploadCompleteService {
    /**
     * 完成图片 Multipart 合并并校验完整对象。
     *
     * @param uploadId 本系统公开的上传会话 ID，不是 OSS uploadId
     * @param request  客户端冗余校验字段
     * @param userId   当前登录用户 ID，由 CurrentUser 提供
     * @return 完成上传的图片 ID
     */
    ImageUploadCompleteVO completeUpload(
            String uploadId,
            ImageUploadCompleteReqDto request,
            Long userId
    );
}
