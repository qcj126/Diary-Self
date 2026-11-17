package diary.file.impl.fileTransactionalservice;

import diary.common.exception.ConflictException;
import diary.common.exception.ParamIllegalException;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ImageUploadSessionPO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.ImageUploadSessionStatusEnum;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ImageUploadSessionMapper;
import diary.file.service.filetransactionalservice.ImageTransactionalService;
import diary.file.state.ImageStatusTransitionPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ImageTransactionalServiceImpl implements ImageTransactionalService {
    private final ImageMapper imageMapper;
    private final ImageUploadSessionMapper sessionMapper;
    private final ImageStatusTransitionPolicy transitionPolicy;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initSessionAndImage(ImagePO imagePO, ImageUploadSessionPO sessionPO) {
        if (imagePO == null || sessionPO == null) {
            throw new ParamIllegalException("图片数据或上传会话数据为空");
        }
        int imageCnt = imageMapper.singleAddImageToDb(imagePO);
        int sessionCnt = sessionMapper.insert(sessionPO);
        if (imageCnt != 1 || sessionCnt != 1) {
            throw new IllegalStateException (
                    "图片数据或会话数据插入失败，图片插入数：" + imageCnt + ", 会话插入数：" + sessionCnt
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeMultipartUpload(String uploadId, Long userId, Long imageId) {
        ImageUploadSessionPO session = sessionMapper.selectByUploadIdAndUserId(uploadId, userId);
        ImagePO image = imageMapper.selectForUploadByIdAndUserId(imageId, userId);

        if (session == null || image == null || !imageId.equals(session.getImageId())) {
            throw new ConflictException("上传会话与图片记录不一致");
        }

        ImageStatusEnum imageStatus = ImageStatusEnum.fromCode(image.getStatus());

        if (Objects.equals(session.getStatus(), ImageUploadSessionStatusEnum.COMPLETED.getCode())) {
            if (imageStatus == ImageStatusEnum.UPLOADED || imageStatus == ImageStatusEnum.READY) {
                return;
            }
            throw new ConflictException("会话已完成，但图片状态不一致");
        }

        if (!Objects.equals(session.getStatus(), ImageUploadSessionStatusEnum.COMPLETING.getCode())) {
            throw new ConflictException("上传会话不处于合并中状态");
        }

        if (imageStatus == ImageStatusEnum.UPLOADING) {
            if (!transitionPolicy.canTransition(imageStatus, ImageStatusEnum.UPLOADED)) {
                throw new ConflictException("图片状态不允许迁移到UPLOADED");
            }

            int imageUpdated = imageMapper.updateStatusIfCurrent(imageId, imageStatus.getCode(), ImageStatusEnum.UPLOADED.getCode());

            if (imageUpdated != 1) {
                throw new ConflictException("图片状态并发更新冲突");
            }
        } else if (imageStatus != ImageStatusEnum.UPLOADED && imageStatus != ImageStatusEnum.READY) {
            throw new ConflictException("图片当前状态不能完成上传：" + imageStatus.name());
        }

        int sessionUpdated = sessionMapper.updateStatusIfVersion(
                uploadId, userId, ImageUploadSessionStatusEnum.COMPLETING.getCode(),
                ImageUploadSessionStatusEnum.COMPLETED.getCode(), session.getVersion()
        );

        if (sessionUpdated != 1) {
            ImageUploadSessionPO latest = sessionMapper.selectByUploadIdAndUserId(uploadId, userId);

            if (latest != null && Objects.equals(latest.getStatus(), ImageUploadSessionStatusEnum.COMPLETED.getCode())) {
                return;
            }

            throw new ConflictException("上传会话最终状态并发更新冲突");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void failMultipartUpload(String uploadId, Long userId, Long imageId) {
        ImageUploadSessionPO session = sessionMapper.selectByUploadIdAndUserId(uploadId, userId);
        ImagePO image = imageMapper.selectForUploadByIdAndUserId(imageId, userId);

        if (session == null || image == null || !imageId.equals(session.getImageId())) {
            throw new ConflictException("上传会话与图片记录不一致");
        }

        ImageStatusEnum imageStatus = ImageStatusEnum.fromCode(image.getStatus());

        if (Objects.equals(session.getStatus(), ImageUploadSessionStatusEnum.ABORTED.getCode())) {
            if (imageStatus == ImageStatusEnum.UPLOAD_FAILED) {
                return;
            }
            throw new ConflictException("会话已中止，但图片状态不一致");
        }

        if (!Objects.equals(session.getStatus(), ImageUploadSessionStatusEnum.COMPLETING.getCode())) {
            throw new ConflictException("上传会话不处于合并中状态");
        }

        if (imageStatus != ImageStatusEnum.UPLOAD_FAILED) {
            if (!transitionPolicy.canTransition(imageStatus, ImageStatusEnum.UPLOAD_FAILED)) {
                throw new ConflictException("图片状态不允许迁移到UPLOAD_FAILED");
            }

            int imageUpdated = imageMapper.updateStatusIfCurrent(imageId, imageStatus.getCode(), ImageStatusEnum.UPLOAD_FAILED.getCode());

            if (imageUpdated != 1) {
                throw new ConflictException("图片失败状态并发更新冲突");
            }
        }

        int sessionUpdated = sessionMapper.updateStatusIfVersion(
                uploadId, userId, ImageUploadSessionStatusEnum.COMPLETING.getCode(),
                ImageUploadSessionStatusEnum.ABORTED.getCode(), session.getVersion()
        );

        if (sessionUpdated != 1) {
            throw new ConflictException("上传会话失败状态并发更新冲突");
        }
    }
}
