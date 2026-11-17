package diary.file.impl.uploadserviceImpl;

import diary.common.exception.ConflictException;
import diary.common.exception.CustomException;
import diary.common.exception.NullResultException;
import diary.common.exception.ParamIllegalException;
import diary.file.entity.model.OssUploadedPart;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ImageUploadChunkPO;
import diary.file.entity.po.ImageUploadSessionPO;
import diary.file.enums.ImageUploadSessionStatusEnum;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ImageUploadChunkMapper;
import diary.file.mapper.ImageUploadSessionMapper;
import diary.file.service.oss.OssStorageService;
import diary.file.service.uploadservice.ImageChunkUploadService;
import diary.file.utils.FileUtil;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

import static diary.file.consts.ImageConst.JPEG_MIME_TYPE;
import static diary.file.consts.ImageConst.JPEG_SIGNATURE;
import static diary.file.consts.ImageConst.PNG_MIME_TYPE;
import static diary.file.consts.ImageConst.PNG_SIGNATURE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageChunkUploadServiceImpl implements ImageChunkUploadService {

    private final ImageUploadSessionMapper sessionMapper;
    private final ImageUploadChunkMapper chunkMapper;
    private final ImageMapper imageMapper;
    private final OssStorageService ossStorageService;
    private final FileUtil fileUtil;

    @Override
    public void uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk, String chunkSha256, Long userId) {
        MyUtils.check()
                .notEmpty(uploadId, "uploadId")
                .notNull(chunk, "chunk")
                .notEmpty(chunkSha256, "chunkSha256")
                .notNull(userId, "userId");

        /*校验sha256*/
        fileUtil.isSha256Valid(chunkSha256);

        /*
         * 查询上传会话必须同时使用 uploadId 和当前登录 userId 查询。
         * 查询不到时不区分“会话不存在”和“属于其他用户”，避免泄露会话信息。
         */
        ImageUploadSessionPO uploadSessionPO = sessionMapper.selectByUploadIdAndUserId(uploadId, userId);

        /*校验session，校验chunkIndex*/
        validateSession(uploadSessionPO);
        validateChunkIndex(chunkIndex, uploadSessionPO.getTotalChunks());

        /*计算期望的chunkSize，然后与前端传入的chunkSize进行对比*/
        long expectedChunkSize = calculateExpectedChunkSize(
                chunkIndex,
                uploadSessionPO.getTotalChunks(),
                uploadSessionPO.getChunkSize(),
                uploadSessionPO.getFileSize()
        );

        /*
         * 只有在HTTP请求头声明大小已经通过校验后才读取内容。
         * 每个请求最多读取一个会话权威 chunkSize 大小的 byte[]。
         * 然后再次与期望的chunkSize进行校验，校验实际文件大小
         */
        if (chunk.getSize() != expectedChunkSize) {
            throw new ParamIllegalException("分片大小不正确，期望：" + expectedChunkSize + "，实际：" + chunk.getSize());
        }
        byte[] chunkBytes;
        try {
            chunkBytes = chunk.getBytes();
            if (chunkBytes.length != expectedChunkSize) {
                throw new ParamIllegalException("分片实际内容大小不正确，期望：" + expectedChunkSize + "，实际：" + chunkBytes.length);
            }
        } catch (IOException exception) {
            log.error("读取图片分片失败，uploadId={}, chunkIndex={}", uploadId, chunkIndex, exception);
            throw new CustomException("读取图片分片失败" + "失败原因：" + exception);
        }
        /*
         * 使用读取的byte，计算sha256，然后与入参sha256进行比对
         * */
        String actualSha256 = calculateSha256(chunkBytes);
        if (!actualSha256.equals(chunkSha256)) {
            throw new ParamIllegalException("分片SHA-256校验失败");
        }

        /*
         * 当文件分片是第一个时，校验图片的魔数
         * 因为第一个分片中必然存有整个图片的头信息
         * */
        if (chunkIndex == 0) {
            // 查看图片数据类型
            ImagePO imagePO = imageMapper.selectForUploadByIdAndUserId(uploadSessionPO.getImageId(), userId);
            if (imagePO == null) {
                throw new NullResultException("imagePO为空，图片id为：" + uploadSessionPO.getImageId());
            }
            String mimeType = imagePO.getMimeType();
            boolean magicMatches = switch (mimeType) {
                case PNG_MIME_TYPE -> startsWith(chunkBytes, PNG_SIGNATURE);
                case JPEG_MIME_TYPE -> startsWith(chunkBytes, JPEG_SIGNATURE);
                default -> false;
            };
            if (!magicMatches) {
                throw new ParamIllegalException("图片文件魔数与mimeType不匹配");
            }
        }

        /*
         * OSS上传前先查询数据库。
         * 相同内容直接按幂等成功处理，不重复请求OSS。
         */
        ImageUploadChunkPO chunkPO = chunkMapper.selectByUploadIdAndChunkIndex(uploadId, chunkIndex);
        if (chunkPO != null) {
            // 看是否分片内容相同
            String dbChunkSha256 = chunkPO.getChunkSha256();
            if (!dbChunkSha256.equals(chunkSha256) || chunkPO.getChunkSize() != chunkBytes.length) {
                throw new ConflictException("该chunkIndex已存在不同内容的分片");
            }
            return;
        }

        /*
        * 上传分片到OSS，然后将分片数据入库
        * */
        int partNumber = Math.addExact(chunkIndex, 1);
        InputStream inputStream = new ByteArrayInputStream(chunkBytes);
        OssUploadedPart uploadPart = ossStorageService.uploadPart(uploadSessionPO.getObjectKey(), uploadSessionPO.getOssUploadId(), partNumber, inputStream, expectedChunkSize);

        /*封装chunk数据并入库*/
        ImageUploadChunkPO uploadChunkPO = ImageUploadChunkPO.builder()
                .uploadId(uploadId)
                .chunkIndex(chunkIndex)
                .partNumber(partNumber)
                .chunkSha256(actualSha256)
                .etag(uploadPart.etag())
                .chunkSize(chunkBytes.length)
                .build();
        try {
            int chunkCnt = chunkMapper.insert(uploadChunkPO);
            if (chunkCnt != 1) {
                throw new CustomException("分片记录写入失败");
            }
        } catch (DuplicateKeyException duplicateKeyException) {
            ImageUploadChunkPO concurrentChunk = chunkMapper.selectByUploadIdAndChunkIndex(uploadId, chunkIndex);
            /*
             * 如果按chunkIndex查询不到，说明可能是其他唯一索引或
             * 非预期主键冲突，不能一律当作幂等成功。
             */
            if (concurrentChunk == null) {
                throw duplicateKeyException;
            }

            if (!concurrentChunk.getChunkSha256().equals(chunkSha256) || concurrentChunk.getChunkSize() != chunkBytes.length) {
                throw new ConflictException("该chunkIndex已存在不同内容的分片");
            }
        }
    }

    private long calculateExpectedChunkSize(int chunkIndex, Integer totalChunks, Integer chunkSize, Long fileSize) {
        /*若不是最后一片，那么每一片都是满的*/
        if (chunkIndex < totalChunks - 1) return chunkSize;
        /*计算最后一片之前的分片总字节数*/
        long precedingChunksSize = Math.multiplyExact((long) totalChunks - 1L, (long) chunkSize);
        /*获取最后一块分片字节数*/
        long lastChunkSize = Math.subtractExact(fileSize, precedingChunksSize);
        if (lastChunkSize < 1 || lastChunkSize > chunkSize) {
            throw new CustomException("上传会话最后一片大小异常");
        }

        return lastChunkSize;
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) return false;

        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) return false;
        }

        return true;
    }

    private void validateChunkIndex(int chunkIndex, Integer totalChunks) {
        if (chunkIndex < 0 || chunkIndex > totalChunks -1) {
            throw new ParamIllegalException("chunkIndex必须在0到" + (totalChunks - 1) + "之间");
        }
        if (totalChunks > 10000) {
            throw new ParamIllegalException("总分片数最多10000片");
        }
    }

    private void validateSession(ImageUploadSessionPO uploadSessionPO) {
        if (uploadSessionPO == null) {
            throw new ParamIllegalException("上传会话不存在或无权访问");
        }
        if (!Objects.equals(uploadSessionPO.getStatus(), ImageUploadSessionStatusEnum.UPLOADING.getCode())) {
            throw new ConflictException("上传会话当前状态不允许上传");
        }
        if (uploadSessionPO.getExpiresAt() == null || !uploadSessionPO.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ConflictException("上传会话已过期");
        }
        if (uploadSessionPO.getImageId() == null
                || uploadSessionPO.getObjectKey() == null
                || uploadSessionPO.getObjectKey().isBlank()
                || uploadSessionPO.getOssUploadId() == null
                || uploadSessionPO.getOssUploadId().isBlank()
                || uploadSessionPO.getFileSize() == null
                || uploadSessionPO.getFileSize() <= 0
                || uploadSessionPO.getChunkSize() == null
                || uploadSessionPO.getChunkSize() <= 0
                || uploadSessionPO.getTotalChunks() == null
                || uploadSessionPO.getTotalChunks() <= 0) {
            throw new CustomException("上传会话数据不完整");
        }

        long expectedTotalChunks =
                ((uploadSessionPO.getFileSize() - 1L) / uploadSessionPO.getChunkSize()) + 1L;

        if (expectedTotalChunks != uploadSessionPO.getTotalChunks()) {
            throw new CustomException("上传会话分片参数不一致");
        }
    }

    private String calculateSha256(byte[] chunkBytes) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(messageDigest.digest(chunkBytes));
        } catch (NoSuchAlgorithmException exception) {
            log.error("当前JVM不支持SHA-256", exception);
            throw new CustomException("服务器无法计算分片SHA-256");
        }
    }
}
