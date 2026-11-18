package diary.file.impl.uploadserviceImpl;

import diary.common.exception.ParamIllegalException;
import diary.file.entity.dto.ImageUploadInitReqDto;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ImageUploadChunkPO;
import diary.file.entity.po.ImageUploadSessionPO;
import diary.file.entity.vo.ImageUploadConfigVO;
import diary.file.entity.vo.ImageUploadInitVO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.ImageTypeEnum;
import diary.file.enums.ImageUploadSessionStatusEnum;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ImageUploadChunkMapper;
import diary.file.mapper.ImageUploadSessionMapper;
import diary.file.property.ImageUploadProperties;
import diary.file.service.filetransactionalservice.ImageTransactionalService;
import diary.file.service.oss.OssStorageService;
import diary.file.service.uploadservice.ImageUploadInitService;
import diary.file.utils.FileUtil;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static diary.file.consts.ImageConst.ALLOWED_EXTENSIONS;
import static diary.file.consts.ImageConst.ALLOWED_MIME_TYPES;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadInitServiceImpl implements ImageUploadInitService {

    private final ImageUploadProperties imageUploadProperties;
    private final FileUtil fileUtil;
    private final ImageMapper imageMapper;
    private final ImageUploadSessionMapper sessionMapper;
    private final ImageUploadChunkMapper chunkMapper;
    private final OssStorageService ossStorageService;
    private final ImageTransactionalService transactionalService;

    @Override
    public ImageUploadInitVO initialize(ImageUploadInitReqDto request, Long userId) {
        /*校验参数*/
        /*校验参数合法性*/
        MyUtils.check()
                .notNull(userId, "userId")
                .notNull(request, "request")
                .notNull(request.getImageTypeCode(), "imageType")
                .notNull(request.getFileSize(), "fileSize")
                .notEmpty(request.getMimeType(), "mimeType")
                .notEmpty(request.getSha256(), "sha256")
                .notNull(request.getChunkSize(), "chunkSize")
                .notEmpty(request.getOriginalName(), "originalName")
                .notNull(request.getTotalChunks(), "totalChunks");

        if (!ALLOWED_MIME_TYPES.contains(request.getMimeType())) {
            throw new ParamIllegalException("mimeType只允许image/png或image/jpeg");
        }
        String requestSha256 = request.getSha256();
        fileUtil.isSha256Valid(requestSha256);
        Integer chunkSize = request.getChunkSize();
        if (!chunkSize.equals(imageUploadProperties.getChunkSize())) {
            throw new ParamIllegalException("chunkSize与服务端配置不一致，服务端要求：" + imageUploadProperties.getChunkSize());
        }
        if (request.getFileSize() <= 0 || request.getFileSize() > imageUploadProperties.getMaxImageSize()) {
            throw new ParamIllegalException("fileSize必须在1到" + imageUploadProperties.getMaxImageSize() + "字节之间");
        }
        String safeOriginalName = fileUtil.getSafeOriginalName(request.getOriginalName());

        int dotIndex = safeOriginalName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == safeOriginalName.length() - 1) {
            throw new ParamIllegalException("文件名缺少有效扩展名");
        }
        int expectedTotalChunks = Math.toIntExact(
                ((request.getFileSize() - 1) / imageUploadProperties.getChunkSize()) + 1
        );
        String extension = safeOriginalName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ParamIllegalException("文件扩展名只允许png、jpg或jpeg");
        }
        if (!matchesMimeType(extension, request.getMimeType())) {
            throw new ParamIllegalException("文件扩展名与mimeType不匹配");
        }

        if (expectedTotalChunks != request.getTotalChunks()) {
            throw new ParamIllegalException("totalChunks计算错误，期望：" + expectedTotalChunks);
        }

        int imageType = ImageTypeEnum.fromCode(request.getImageTypeCode()).getCode();

        /*实现秒传与断点续传的查询*/
        ImageUploadInitVO uploadInitVO = findExistingUpload(userId, imageType, request.getSha256());
        if (uploadInitVO != null) return uploadInitVO;

        /*新增图片与会话数据*/
        try {
            /*初始化OSS并插入image和session*/
            return createNewUpload(userId, request, imageType, safeOriginalName, extension, expectedTotalChunks);
        } catch (DuplicateKeyException duplicateKeyException) {
            /*
             * createNewUpload已经Abort了本实例创建的OSS会话。
             * 这里重新查询并发竞争中由另一实例创建的数据。
             */
            ImageUploadInitVO concurrentResult = findExistingUpload(userId, imageType, request.getSha256());

            if (concurrentResult != null) return concurrentResult;

            /*
             * 可能是object_key、upload_id等其他唯一索引冲突，
             * 不能一律伪装成秒传成功。
             */
            throw duplicateKeyException;
        }

    }

    @Override
    public ImageUploadConfigVO getUploadConfig() {
        return ImageUploadConfigVO.builder()
                .chunkSize(imageUploadProperties.getChunkSize())
                .maxImageSize(imageUploadProperties.getMaxImageSize())
                .allowedMimeTypes(List.of("image/png", "image/jpeg"))
                .allowedExtensions(List.of("png", "jpg", "jpeg"))
                .build();
    }

    /*实现 READY 秒传与断点续传查询*/
    private ImageUploadInitVO findExistingUpload(Long userId, Integer imageType, String sha256) {
        /*查询同用户、同类型、同SHA且状态为READY的图片*/
        ImagePO imagePO = imageMapper.selectReadyByUserIdAndTypeAndSha256(userId, imageType, sha256);

        /*有ready图片时，直接返回结果*/
        if (imagePO != null) {
            log.info("有ready图片，成功触发秒传，图片id = {}", imagePO.getId());
            return ImageUploadInitVO.builder()
                    .imageId(imagePO.getId())
                    .instantUpload(true)
                    .uploadedChunkIndexes(List.of())
                    .build();
        }

        /*没有ready图片时，查询同条件下未完成的图片*/
        imagePO = imageMapper.selectByUserIdAndTypeAndSha256(userId, imageType, sha256);

        /*如果为空，那么这期间没有图片上传，返回空*/
        if (imagePO == null) {
            log.info("没有上传中的图片");
            return null;
        }

        /*如果有图片上传，只有处于上传阶段的图片才能恢复会话上传分片*/
        if (!Objects.equals(imagePO.getStatus(), ImageStatusEnum.UPLOADING.getCode()) &&
            !Objects.equals(imagePO.getStatus(), ImageStatusEnum.PENDING_UPLOAD.getCode())
        ) {
            log.warn("图片状态不是可上传或上传中，图片当前状态为：{}，图片id = {}", imagePO.getStatus(), imagePO.getId());
            return null;
        }

        /*查询当前图片的分片上传会话数据*/
        ImageUploadSessionPO uploadSessionPO = sessionMapper.selectByImageIdAndUserId(imagePO.getId(), userId);

        if(uploadSessionPO == null) {
            log.warn("当前图片无会话信息，图片id = {}", imagePO.getId());
            return null;
        }
        /*如果会话状态已经不处于上传中，那么此会话失效，不可再进行图片上传*/
        if (!Objects.equals(uploadSessionPO.getStatus(), ImageUploadSessionStatusEnum.UPLOADING.getCode())) {
            log.warn("图片会话状态不是可上传状态，图片id = {}， 会话状态 = {}", imagePO.getId(), uploadSessionPO.getStatus());
            return null;
        }

        /*查看会话是否过期，过期直接返回null，不进行过期会话的激活*/
        if (uploadSessionPO.getExpiresAt() == null || !uploadSessionPO.getExpiresAt().isAfter(LocalDateTime.now())) {
            log.warn("图片上传会话已过期，图片id = {}", imagePO.getId());
            return null;
        }

        /*所有校验通过后，返回之前上传的分片信息*/
        List<ImageUploadChunkPO> chunkPOS = chunkMapper.selectByUploadIdOrderByPartNumber(uploadSessionPO.getUploadId());
        List<Integer> uploadedChunkIndexes = new ArrayList<>();
        for (ImageUploadChunkPO chunkPO : chunkPOS) {
            uploadedChunkIndexes.add(chunkPO.getChunkIndex());
        }
        return ImageUploadInitVO.builder()
                .imageId(imagePO.getId())
                .uploadId(uploadSessionPO.getUploadId())
                .instantUpload(false)
                .chunkSize(uploadSessionPO.getChunkSize())
                .totalChunks(uploadSessionPO.getTotalChunks())
                .uploadedChunkIndexes(uploadedChunkIndexes)
                .build();
    }

    /*实现新文件的 OSS 初始化与事务落库*/
    private ImageUploadInitVO createNewUpload(Long userId, ImageUploadInitReqDto request, Integer imageType,
                                             String safeOriginalName, String extension, Integer expectedTotalChunks) {
        /*准备前置业务数据*/
        long imageId = MyUtils.getPrimaryKey();
        String uploadId = UUID.randomUUID().toString();
        String objectKey = "images/original/" + userId + "/" + imageType + "/"
                + imageId + "-" + request.getSha256() + "." + extension;
        /*初始化oss上传*/
        String ossUploadId = ossStorageService.initiateMultipartUpload(objectKey, request.getMimeType());
        if (ossUploadId == null || ossUploadId.isBlank()) {
            throw new ParamIllegalException("ossUploadId为空");
        }

        /*构建实体类*/
        ImagePO imagePO = new ImagePO();
        imagePO.setId(imageId);
        imagePO.setUserId(userId);
        imagePO.setFileSize(request.getFileSize());
        imagePO.setObjectKey(objectKey);
        imagePO.setOriginalName(safeOriginalName);
        imagePO.setMimeType(request.getMimeType());
        imagePO.setType(imageType);
        imagePO.setDeleted(0);
        imagePO.setStatus(ImageStatusEnum.PENDING_UPLOAD.getCode());
        imagePO.setSha256(request.getSha256());

        ImageUploadSessionPO sessionPO = ImageUploadSessionPO.builder()
                .uploadId(uploadId)
                .imageId(imageId)
                .userId(userId)
                .objectKey(objectKey)
                .status(ImageUploadSessionStatusEnum.UPLOADING.getCode())
                .sha256(request.getSha256())
                .fileSize(request.getFileSize())
                .chunkSize(imageUploadProperties.getChunkSize())
                .totalChunks(expectedTotalChunks)
                .ossUploadId(ossUploadId)
                .version(0)
                .expiresAt(LocalDateTime.now().plusHours(imageUploadProperties.getSessionTtlHours()))
                .build();

        /*插入图片数据和图片上传会话*/
        try {
            transactionalService.initSessionAndImage(imagePO, sessionPO);
        } catch (RuntimeException databaseException) {
            /*
             * 数据库事务已经回滚。
             * 现在补偿删除本实例刚创建的OSS Multipart会话。
             */
            try {
                ossStorageService.abortMultipartUpload(objectKey, ossUploadId);
            } catch (RuntimeException abortException) {
                /*
                 * 保留数据库原始异常，Abort异常作为suppressed附加。
                 * 否则下一步将无法识别DuplicateKeyException。
                 */
                databaseException.addSuppressed(abortException);
            }

            throw databaseException;
        }
        return ImageUploadInitVO.builder()
                .imageId(imagePO.getId())
                .uploadId(uploadId)
                .instantUpload(false)
                .chunkSize(imageUploadProperties.getChunkSize())
                .totalChunks(expectedTotalChunks)
                .uploadedChunkIndexes(List.of())
                .build();
    }

    private boolean matchesMimeType(String extension, String mimeType) {
        return switch (mimeType) {
            case "image/png" -> "png".equals(extension);
            case "image/jpeg" -> "jpg".equals(extension) || "jpeg".equals(extension);
            default -> false;
        };
    }
}
