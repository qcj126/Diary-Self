package diary.file.impl.uploadserviceImpl;

import diary.common.exception.ConflictException;
import diary.common.exception.CustomException;
import diary.common.exception.ParamIllegalException;
import diary.file.entity.dto.ImageUploadCompleteReqDto;
import diary.file.entity.model.OssCompleteResult;
import diary.file.entity.model.OssObjectResource;
import diary.file.entity.model.OssUploadedPart;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ImageUploadChunkPO;
import diary.file.entity.po.ImageUploadSessionPO;
import diary.file.entity.vo.ImageUploadCompleteVO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.ImageTransitionResultEnum;
import diary.file.enums.ImageUploadSessionStatusEnum;
import diary.file.enums.OssCompleteState;
import diary.file.enums.OssOperationEnum;
import diary.file.exception.OssStorageException;
import diary.file.exception.OssObjectSizeMismatchException;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ImageUploadChunkMapper;
import diary.file.mapper.ImageUploadSessionMapper;
import diary.file.service.ImageStatusService;
import diary.file.service.filetransactionalservice.ImageTransactionalService;
import diary.file.service.oss.OssStorageService;
import diary.file.service.uploadservice.ImageUploadCompleteService;
import diary.file.utils.FileUtil;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageUploadCompleteServiceImpl implements ImageUploadCompleteService {
    private final FileUtil fileUtil;
    private final ImageUploadSessionMapper sessionMapper;
    private final ImageMapper imageMapper;
    private final ImageUploadChunkMapper chunkMapper;
    private final OssStorageService ossStorageService;
    private final ImageTransactionalService transactionalService;
    private final ImageStatusService imageStatusService;

    private static final int HASH_BUFFER_SIZE = 32 * 1024;

    @Override
    public ImageUploadCompleteVO completeUpload(String uploadId, ImageUploadCompleteReqDto request, Long userId) {
        /*校验基础请求参数*/
        MyUtils.check()
                .notEmpty(uploadId, "uploadId")
                .notNull(userId, "userId")
                .notNull(request, "request")
                .notNull(request.getImageId(), "imageId")
                .notEmpty(request.getSha256(), "sha256")
                .notNull(request.getTotalChunks(), "totalChunks");
        Integer totalChunks = request.getTotalChunks();
        if (totalChunks < 1 || totalChunks > 10000) {
            throw new ParamIllegalException("总分片数最多10000片");
        }
        /*校验sha256格式*/
        String sha256 = request.getSha256();
        fileUtil.isSha256Valid(sha256);

        /*按 uploadId + userId 查询会话*/
        ImageUploadSessionPO sessionPO = sessionMapper.selectByUploadIdAndUserId(uploadId, userId);
        /*校验session数据完整性*/
        validateSession(sessionPO);
        /*比较请求冗余字段与会话，防止传到后端的为脏数据*/
        String sessionPOUploadId = sessionPO.getUploadId();
        String sessionPOSha256 = sessionPO.getSha256();
        Integer sessionPOTotalChunks = sessionPO.getTotalChunks();
        if (!sha256.equals(sessionPOSha256)
                || !uploadId.equals(sessionPOUploadId)
                || !Objects.equals(request.getImageId(), sessionPO.getImageId())
                || !Objects.equals(totalChunks, sessionPOTotalChunks)) {
            throw new ConflictException("请求数据与期望数据不一致");
        }
        /*并发请求中，如果状态已为COMLETED，那么进入幂等检查*/
        ImageUploadSessionStatusEnum imageUploadSessionStatusEnum = ImageUploadSessionStatusEnum.fromCode(sessionPO.getStatus());
        if (imageUploadSessionStatusEnum == ImageUploadSessionStatusEnum.COMPLETED) {
            /*会话状态COMPLETED对应图片状态READY和UPLOADED*/
            return completedResponse(sessionPO, userId);
        }
        /*如果会话状态为INITIATING、ABORTED、EXPIRED，则直接拒绝*/
        if (imageUploadSessionStatusEnum == ImageUploadSessionStatusEnum.INITIATING ||
                imageUploadSessionStatusEnum == ImageUploadSessionStatusEnum.ABORTED ||
                imageUploadSessionStatusEnum == ImageUploadSessionStatusEnum.EXPIRED) {
            throw new ConflictException("当前会话节点不允许合并分片文件");
        }

        if (imageUploadSessionStatusEnum == ImageUploadSessionStatusEnum.UPLOADING) {
            if (sessionPO.getExpiresAt() == null || !sessionPO.getExpiresAt().isAfter(LocalDateTime.now())) {
                throw new ConflictException("上传会话已过期");
            }
        } else if (imageUploadSessionStatusEnum != ImageUploadSessionStatusEnum.COMPLETING) {
            throw new ConflictException("当前会话状态不允许合并分片文件");
        }

        /*校验每一片分片，验证文件正确性以及完整性*/
        List<OssUploadedPart> partList = loadAndValidateParts(sessionPO);

        /*将正在处于上传中的会话状态，循环等待至COMPLETING*/
        ImageUploadSessionPO session = enterCompleting(sessionPO, request, userId);

        if (Objects.equals(session.getStatus(), ImageUploadSessionStatusEnum.COMPLETED.getCode())) {
            return completedResponse(session, userId);
        }

        ensureImageUploadingOrLater(session, userId);

        OssCompleteResult completeResult;
        try {
            completeResult = ossStorageService.completeMultipartUpload(
                    session.getObjectKey(),
                    session.getOssUploadId(),
                    partList,
                    session.getFileSize()
            );
        } catch (OssStorageException storageException) {
            if (!(storageException.getCause() instanceof OssObjectSizeMismatchException sizeMismatchException)) {
                throw storageException;
            }

            ImageIntegrityException integrityException =
                    new ImageIntegrityException(sizeMismatchException.getMessage());
            integrityException.addSuppressed(storageException);
            compensateIntegrityFailure(session, userId, integrityException);
            throw integrityException;
        }

        validateCompleteResult(completeResult);

        try {
            validateCompletedObject(session);
        } catch (ImageIntegrityException integrityException) {
            compensateIntegrityFailure(session, userId, integrityException);
            throw integrityException;
        }

        /*
         * 此方法内部只有数据库操作。
         * OSS Complete 和对象流校验均已在事务外结束。
         */
        transactionalService.completeMultipartUpload(session.getUploadId(), userId, session.getImageId());

        return ImageUploadCompleteVO.builder()
                .imageId(session.getImageId())
                .build();
    }

    private MessageDigest newSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前JVM不支持SHA-256", exception);
        }
    }

    private void compensateIntegrityFailure(ImageUploadSessionPO session, Long userId, ImageIntegrityException integrityException) {
        /*
         * 先原子记录失败状态。
         * 如果数据库失败，保留错误对象及COMPLETING状态，客户端仍可重试。
         */
        try {
            transactionalService.failMultipartUpload(session.getUploadId(), userId, session.getImageId());
        } catch (RuntimeException databaseException) {
            integrityException.addSuppressed(databaseException);
            throw integrityException;
        }

        /*
         * 数据库已经记录为ABORTED/UPLOAD_FAILED后再删除对象。
         * 删除失败时，UPLOAD_FAILED图片仍保留objectKey，可由现有清理流程恢复。
         */
        try {
            ossStorageService.deleteObject(session.getObjectKey());
        } catch (RuntimeException deleteException) {
            integrityException.addSuppressed(deleteException);
        }
    }

    private void validateCompletedObject(ImageUploadSessionPO session) {
        MessageDigest digest = newSha256Digest();
        try (OssObjectResource resource = ossStorageService.openObject(session.getObjectKey())) {

            if (resource.getContentLength() != session.getFileSize()) {
                throw new ImageIntegrityException(
                        "OSS完整对象大小与上传会话不一致"
                );
            }

            InputStream inputStream = resource.getInputStream();
            byte[] buffer = new byte[HASH_BUFFER_SIZE];
            int readLength;

            while ((readLength = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, readLength);
            }
        } catch (IOException exception) {
            throw new OssStorageException(
                    OssOperationEnum.GET_OBJECT.name(),
                    session.getObjectKey(),
                    exception
            );
        }

        String actualSha256 = HexFormat.of().formatHex(digest.digest());

        if (!actualSha256.equals(session.getSha256())) {
            throw new ImageIntegrityException("完整文件SHA-256校验失败");
        }
    }

    private static final class ImageIntegrityException extends ConflictException {
        private ImageIntegrityException(String message) {
            super(message);
        }
    }

    private void validateCompleteResult(OssCompleteResult result) {
        if (result == null || result.state() == null) {
            throw new CustomException("OSS Complete没有返回有效结果");
        }

        if (result.state() != OssCompleteState.COMPLETED && result.state() != OssCompleteState.ALREADY_COMPLETED) {
            throw new CustomException("未知OSS Complete结果：" + result.state());
        }
    }

    private ImageUploadCompleteVO completedResponse(ImageUploadSessionPO session, Long userId) {
        ImagePO image = imageMapper.selectForUploadByIdAndUserId(session.getImageId(), userId);

        if (image == null) {
            throw new ConflictException("上传会话已完成，但图片不存在或不属于当前用户");
        }

        ImageStatusEnum imageStatus = ImageStatusEnum.fromCode(image.getStatus());

        if (imageStatus != ImageStatusEnum.UPLOADED && imageStatus != ImageStatusEnum.READY) {
            throw new ConflictException("上传会话已完成，但图片状态不一致：" + imageStatus.name());
        }

        return ImageUploadCompleteVO.builder().imageId(session.getImageId()).build();
    }

    private void ensureImageUploadingOrLater(ImageUploadSessionPO session, Long userId) {
        ImageTransitionResultEnum result = imageStatusService.markUploading(session.getImageId());

        if (result != ImageTransitionResultEnum.REJECTED) {
            return;
        }

        /*
         * UPLOADED/READY 可能来自另一实例已完成或旧的不一致状态。
         * 最终事务会再次核对 session 和 image。
         */
        ImagePO image = imageMapper.selectForUploadByIdAndUserId(
                session.getImageId(),
                userId
        );

        if (image == null) {
            throw new ConflictException("图片不存在或不属于当前用户");
        }

        ImageStatusEnum status = ImageStatusEnum.fromCode(image.getStatus());

        if (status != ImageStatusEnum.UPLOADED
                && status != ImageStatusEnum.READY) {
            throw new ConflictException(
                    "图片当前状态不允许完成上传：" + status.name()
            );
        }
    }

    private void validateSession(ImageUploadSessionPO session) {
        if (session == null) {
            throw new ParamIllegalException("上传会话不存在或无权访问");
        }
        if (session.getUploadId() == null
                || session.getUploadId().isBlank()
                || session.getImageId() == null
                || session.getImageId() <= 0
                || session.getUserId() == null
                || session.getStatus() == null
                || session.getVersion() == null
                || session.getVersion() < 0
                || session.getObjectKey() == null
                || session.getObjectKey().isBlank()
                || session.getOssUploadId() == null
                || session.getOssUploadId().isBlank()
                || session.getSha256() == null
                || session.getFileSize() == null
                || session.getFileSize() <= 0
                || session.getChunkSize() == null
                || session.getChunkSize() <= 0
                || session.getTotalChunks() == null
                || session.getTotalChunks() <= 0) {
            throw new CustomException("上传会话数据不完整");
        }

        fileUtil.isSha256Valid(session.getSha256());

        long expectedTotalChunks = ((session.getFileSize() - 1L) / session.getChunkSize()) + 1L;

        if (expectedTotalChunks != session.getTotalChunks()) {
            throw new CustomException("上传会话分片参数不一致");
        }
    }

    private List<OssUploadedPart> loadAndValidateParts(ImageUploadSessionPO session) {
        List<ImageUploadChunkPO> chunks = chunkMapper.selectByUploadIdOrderByPartNumber(session.getUploadId());
        if (chunks == null || chunks.size() != session.getTotalChunks()) {
            throw new ConflictException("上传分片数量不完整");
        }

        Set<Integer> chunkIndexes = new HashSet<>();
        Set<Integer> partNumbers = new HashSet<>();
        long totalSize = 0L;

        /*率先校验最后一块分片是否正确，不正确则快速失败*/
        long lastChunkSize = Math.subtractExact(session.getFileSize(),
                Math.multiplyExact((long) session.getTotalChunks() - 1L, (long) session.getChunkSize()));
        if (lastChunkSize <= 0 || lastChunkSize > session.getChunkSize()) {
            throw new CustomException("上传会话最后一片大小异常");
        }

        /*开始正式校验每一个分片*/
        for (int index = 0; index < chunks.size(); index++) {
            ImageUploadChunkPO chunk = chunks.get(index);
            int expectedPartNumber = index + 1;

            if (chunk == null || chunk.getChunkIndex() == null || chunk.getPartNumber() == null ||
                    chunk.getChunkSize() == null || chunk.getChunkSha256() == null || chunk.getEtag() == null) {
                throw new CustomException("上传分片记录不完整");
            }

            if (!chunkIndexes.add(chunk.getChunkIndex())) {
                throw new ConflictException("存在重复chunkIndex：" + chunk.getChunkIndex());
            }

            if (!partNumbers.add(chunk.getPartNumber())) {
                throw new ConflictException("存在重复partNumber：" + chunk.getPartNumber());
            }

            if (chunk.getChunkIndex() != index) {
                throw new ConflictException("chunkIndex不连续，期望：" + index + "，实际：" + chunk.getChunkIndex());
            }

            if (chunk.getPartNumber() != expectedPartNumber) {
                throw new ConflictException("partNumber不连续，期望：" + expectedPartNumber + "，实际：" + chunk.getPartNumber());
            }

            if (chunk.getPartNumber() != chunk.getChunkIndex() + 1) {
                throw new ConflictException("partNumber与chunkIndex关系不正确");
            }

            int expectedSize = index == chunks.size() - 1 ? Math.toIntExact(lastChunkSize) : session.getChunkSize();

            if (chunk.getChunkSize() != expectedSize) {
                throw new ConflictException("分片大小不正确，chunkIndex=" + chunk.getChunkIndex());
            }

            fileUtil.isSha256Valid(chunk.getChunkSha256());

            if (chunk.getEtag().isBlank()) {
                throw new ConflictException("分片ETag为空，chunkIndex=" + chunk.getChunkIndex());
            }

            totalSize = Math.addExact(totalSize, chunk.getChunkSize());
        }

        if (totalSize != session.getFileSize()) {
            throw new ConflictException("全部分片大小之和与文件大小不一致");
        }

        return chunks.stream()
                .map(
                        chunk -> new OssUploadedPart(chunk.getPartNumber(), chunk.getEtag(), chunk.getChunkSize())
                ).toList();
    }

    private ImageUploadSessionPO enterCompleting(ImageUploadSessionPO session, ImageUploadCompleteReqDto request, Long userId) {
        while (true) {
            ImageUploadSessionStatusEnum status = ImageUploadSessionStatusEnum.fromCode(session.getStatus());

            if (status == ImageUploadSessionStatusEnum.COMPLETING || status == ImageUploadSessionStatusEnum.COMPLETED) {
                return session;
            }

            if (status != ImageUploadSessionStatusEnum.UPLOADING) {
                throw new ConflictException("上传会话当前状态不允许完成：" + status.name());
            }

            if (session.getExpiresAt() == null || !session.getExpiresAt().isAfter(LocalDateTime.now())) {
                throw new ConflictException("上传会话已过期");
            }

            int updated = sessionMapper.updateStatusIfVersion(
                    session.getUploadId(),
                    userId,
                    ImageUploadSessionStatusEnum.UPLOADING.getCode(),
                    ImageUploadSessionStatusEnum.COMPLETING.getCode(),
                    session.getVersion()
            );

            ImageUploadSessionPO latestSession = sessionMapper.selectByUploadIdAndUserId(session.getUploadId(), userId);

            if (latestSession == null) {
                throw new ParamIllegalException("上传会话不存在或无权访问");
            }
            validateSession(latestSession);
            if (!request.getSha256().equals(latestSession.getSha256()) ||
                    !request.getImageId().equals(latestSession.getImageId()) ||
                    !Objects.equals(request.getTotalChunks(), latestSession.getTotalChunks())) {
                throw new ConflictException("请求数据与期望数据不一致");
            }

            if (updated == 1) {
                if (!Objects.equals(latestSession.getStatus(), ImageUploadSessionStatusEnum.COMPLETING.getCode())
                        && !Objects.equals(latestSession.getStatus(), ImageUploadSessionStatusEnum.COMPLETED.getCode())) {
                    throw new ConflictException("上传会话状态抢占结果异常");
                }
                return latestSession;
            }

            /*
             * CAS 失败不直接报错，重新根据数据库最新状态判断。
             */
            session = latestSession;
        }
    }
}
