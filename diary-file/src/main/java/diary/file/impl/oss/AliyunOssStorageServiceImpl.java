package diary.file.impl.oss;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import diary.file.entity.model.OssCompleteResult;
import diary.file.entity.model.OssObjectMetadata;
import diary.file.entity.model.OssObjectResource;
import diary.file.entity.model.OssUploadedPart;
import diary.file.enums.OssCompleteState;
import diary.file.enums.OssOperationEnum;
import diary.file.exception.OssStorageException;
import diary.file.property.ImageUploadProperties;
import diary.file.service.oss.OssStorageService;
import diary.utils.common.MyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AliyunOssStorageServiceImpl implements OssStorageService {
    private final String bucketName;
    private final OSS ossClient;
    private final ImageUploadProperties imageUploadProperties;
    private static final int MIN_PART_NUMBER = 1;
    private static final int MAX_PART_NUMBER = 10_000;
    private static final int MAX_CHUNK_SIZE = 1024 * 1024;
    private static final String NO_SUCH_UPLOAD_ERROR_CODE = "NoSuchUpload";
    private static final String NO_SUCH_KEY_ERROR_CODE = "NoSuchKey";

    public AliyunOssStorageServiceImpl(
            OSS ossClient,
            @Value("${aliyun.oss.bucket-name}") String bucketName,
            ImageUploadProperties imageUploadProperties
    ) {
        MyUtils.check()
                .notNull(ossClient, "ossClient")
                .notEmpty(bucketName, "bucketName")
                .notNull(imageUploadProperties, "imageUploadProperties")
                .notNull(imageUploadProperties.getChunkSize(), "chunkSize")
                .notNull(imageUploadProperties.getMaxImageSize(), "maxImageSize");
        if (imageUploadProperties.getChunkSize() <= 0
                || imageUploadProperties.getChunkSize() > MAX_CHUNK_SIZE) {
            throw new IllegalArgumentException("chunkSize必须大于0且不能超过1MB");
        }
        if (imageUploadProperties.getMaxImageSize() <= 0) {
            throw new IllegalArgumentException(
                    "maxImageSize必须大于0"
            );
        }

        this.ossClient = ossClient;
        this.bucketName = bucketName;
        this.imageUploadProperties = imageUploadProperties;
    }

    /*
     * contentType: image/jpeg   image/png
     * 初始化分片上传，获取唯一的uploadId
     */
    @Override
    public String initiateMultipartUpload(String objectKey, String contentType) {
        MyUtils.check()
                .notEmpty(objectKey, "objectKey")
                .notEmpty(contentType, "contentType");
        // 构建Metadata，告诉OSS合并最终结果的文件类型
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(contentType);

        // 构建初始化上传请求
        InitiateMultipartUploadRequest uploadRequest = new InitiateMultipartUploadRequest(
                bucketName, objectKey, objectMetadata
        );

        // 请求OSS并获取返回体
        final InitiateMultipartUploadResult result;
        try {
            result = ossClient.initiateMultipartUpload(uploadRequest);
        } catch (OSSException | ClientException e) {
            throw new OssStorageException(OssOperationEnum.INITIATE_MULTIPART.name(), objectKey, e);
        }
        if (result == null || result.getUploadId() == null || result.getUploadId().isBlank()) {
            throw new OssStorageException(
                    OssOperationEnum.INITIATE_MULTIPART.name(),
                    objectKey,
                    new IllegalStateException("OSS初始化Multipart成功，但没有返回uploadId")
            );
        }
        return result.getUploadId();
    }

    /*将文件分片上传到阿里云*/
    @Override
    public OssUploadedPart uploadPart(String objectKey, String ossUploadId, int partNumber, InputStream inputStream, long partSize) {
        MyUtils.check()
                .notEmpty(objectKey, "objectKey")
                .notEmpty(ossUploadId, "ossUploadId")
                .notNull(inputStream, "inputStream");

        if (partNumber < MIN_PART_NUMBER || partNumber > MAX_PART_NUMBER) {
            throw new IllegalArgumentException(
                    "partNumber必须在1到10000之间"
            );
        }
        if (partSize <= 0 || partSize > imageUploadProperties.getChunkSize()) {
            throw new IllegalArgumentException(
                    "分片大小必须大于0且不能超过配置的chunkSize="
                            + imageUploadProperties.getChunkSize() + "字节"
            );
        }

        UploadPartRequest uploadPartRequest = new UploadPartRequest(
                bucketName, objectKey, ossUploadId, partNumber, inputStream, partSize
        );

        final UploadPartResult result;
        try {
            result = ossClient.uploadPart(uploadPartRequest);
        } catch (OSSException | ClientException e) {
            throw new OssStorageException(OssOperationEnum.UPLOAD_PART.name(), objectKey, e);
        }

        if (result == null || result.getETag() == null || result.getETag().isBlank()) {
            throw new OssStorageException(
                    OssOperationEnum.UPLOAD_PART.name(),
                    objectKey,
                    new IllegalStateException(
                            "OSS分片上传成功，但没有返回ETag"
                    )
            );
        }

        return new OssUploadedPart(
                partNumber, result.getETag(), partSize
        );
    }

    /*通知oss聚合分片文件*/
    @Override
    public OssCompleteResult completeMultipartUpload(String objectKey, String ossUploadId, List<OssUploadedPart> parts, long expectedFileSize) {
        MyUtils.check()
                .notEmpty(objectKey, "objectKey")
                .notEmpty(ossUploadId, "ossUploadId")
                .notNull(parts, "parts")
                .listNotEmpty(parts, "parts")
                .listNotContainsEmpty(parts, "parts");
        List<PartETag> etagList = new ArrayList<>();

        if (expectedFileSize <= 0 || expectedFileSize > imageUploadProperties.getMaxImageSize()) {
            throw new IllegalArgumentException("expectedFileSize必须大于0且不能超过最大图片大小");
        }

        /*对分片列表进行合法校验，并按顺序排列*/
        List<OssUploadedPart> sortedParts = validateAndSortParts(parts, expectedFileSize);

        for (OssUploadedPart part : sortedParts) {
            etagList.add(new PartETag(part.partNumber(), part.etag()));
        }

        CompleteMultipartUploadRequest uploadRequest = new CompleteMultipartUploadRequest(
                bucketName, objectKey, ossUploadId, etagList
        );

        try {
            // 通知oss进行分片聚合
            ossClient.completeMultipartUpload(uploadRequest);
        } catch (OSSException exception) {
            if (NO_SUCH_UPLOAD_ERROR_CODE.equals(exception.getErrorCode())) {
                return recoverCompletedObject(objectKey, expectedFileSize, exception);
            }

            throw new OssStorageException(OssOperationEnum.COMPLETE_MULTIPART.name(), objectKey, exception);
        } catch (ClientException exception) {
            throw new OssStorageException(OssOperationEnum.COMPLETE_MULTIPART.name(), objectKey, exception);
        }

        /*读取聚合后的OSS对象元信息*/
        ObjectMetadata metadata = loadObjectMetadata(objectKey, OssOperationEnum.COMPLETE_MULTIPART);

        /*校验聚合后的文件大小*/
        validateCompletedObjectSize(objectKey, expectedFileSize, metadata.getContentLength());

        return new OssCompleteResult(OssCompleteState.COMPLETED, metadata.getETag(), metadata.getContentLength());
    }

    /*当上传被中断或因网络问题，传了部分到OSS，调用此方法终止会话并释放part数据
    * 场景一：数据库落库失败
    * 场景二：并发初始化失败
    * 场景三：会话过期
    * 场景四：用户取消上传
    * */
    @Override
    public void abortMultipartUpload(String objectKey, String ossUploadId) {
        MyUtils.check()
                .notEmpty(objectKey, "objectKey")
                .notEmpty(ossUploadId, "ossUploadId");

        AbortMultipartUploadRequest uploadRequest = new AbortMultipartUploadRequest(
                bucketName, objectKey, ossUploadId
        );

        try {
            ossClient.abortMultipartUpload(uploadRequest);
        } catch (OSSException exception) {
            if (NO_SUCH_UPLOAD_ERROR_CODE.equals(exception.getErrorCode())) {
                return;
            }
            throw new OssStorageException(OssOperationEnum.ABORT_MULTIPART.name(), objectKey, exception);
        } catch (ClientException exception) {
            throw new OssStorageException(OssOperationEnum.ABORT_MULTIPART.name(), objectKey, exception);
        }
    }

    /*获取meta对象的网络请求头信息*/
    @Override
    public OssObjectMetadata getObjectMetadata(String objectKey) {
        MyUtils.check()
                .notEmpty(objectKey, "objectKey");
        ObjectMetadata metadata = loadObjectMetadata(objectKey, OssOperationEnum.HEAD_OBJECT);
        return new OssObjectMetadata(
                metadata.getContentLength(),
                metadata.getContentType(),
                metadata.getETag()
        );
    }

    /*打开OSS对象正文流，供完整SHA-256计算和缩略图生成使用。*/
    @Override
    public OssObjectResource openObject(String objectKey) {
        MyUtils.check()
                .notEmpty(objectKey, "objectKey");

        final OSSObject ossObject;

        try {
            /*获取oss对象*/
            ossObject = ossClient.getObject(bucketName, objectKey);
        } catch (OSSException | ClientException exception) {
            throw new OssStorageException(OssOperationEnum.GET_OBJECT.name(), objectKey, exception);
        }

        if (ossObject == null) {
            throw new OssStorageException(
                    OssOperationEnum.GET_OBJECT.name(),
                    objectKey,
                    new IllegalStateException("OSS没有返回对象")
            );
        }

        if (ossObject.getObjectContent() == null) {
            throw invalidOpenedObject(objectKey, ossObject, "OSS对象没有返回内容流");
        }

        if (ossObject.getObjectMetadata() == null) {
            throw invalidOpenedObject(objectKey, ossObject, "OSS对象没有返回元信息");
        }

        ObjectMetadata metadata = loadObjectMetadata(objectKey, OssOperationEnum.GET_OBJECT);

        return new OssObjectResource(
                ossObject.getObjectContent(),
                metadata.getContentLength(),
                metadata.getContentType()
        );
    }

    private OssStorageException invalidOpenedObject(
            String objectKey,
            OSSObject ossObject,
            String errorMsg
    ) {
        IllegalStateException cause = new IllegalStateException(errorMsg);

        try {
            ossObject.close();
        } catch (IOException closeException) {
            cause.addSuppressed(closeException);
        }

        return new OssStorageException(OssOperationEnum.GET_OBJECT.name(), objectKey, cause);
    }

    /*只判断对象是否存在，不读取正文*/
    @Override
    public boolean objectExists(String objectKey) {
        MyUtils.check()
                .notEmpty(objectKey, "objectKey");

        try {
            return ossClient.doesObjectExist(bucketName, objectKey);
        } catch (OSSException | ClientException exception) {
            throw new OssStorageException(OssOperationEnum.OBJECT_EXISTS.name(), objectKey, exception);
        }
    }

    /*上传小对象，如缩略图*/
    @Override
    public String putObject(String objectKey, String contentType, InputStream inputStream, long contentLength) {
        MyUtils.check()
                .notEmpty(objectKey, "objectKey")
                .notEmpty(contentType, "contentType")
                .notNull(inputStream, "inputStream");

        if (contentLength <= 0 || contentLength > imageUploadProperties.getMaxImageSize()) {
            throw new IllegalArgumentException("contentLength必须大于0且不能超过最大图片大小");
        }

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);

        final PutObjectResult result;

        try {
            result = ossClient.putObject(bucketName, objectKey, inputStream, metadata);
        } catch (OSSException | ClientException exception) {
            throw new OssStorageException(OssOperationEnum.PUT_OBJECT.name(), objectKey, exception);
        }

        if (result == null || result.getETag() == null || result.getETag().isBlank()) {
            throw new OssStorageException(
                    OssOperationEnum.PUT_OBJECT.name(),
                    objectKey,
                    new IllegalStateException("OSS上传对象成功，但没有返回ETag")
            );
        }

        return result.getETag();
    }

    @Override
    public void deleteObject(String objectKey) {
        MyUtils.check()
                .notEmpty(objectKey, "objectKey");

        try {
            ossClient.deleteObject(bucketName, objectKey);
        } catch (OSSException exception) {
            if (NO_SUCH_KEY_ERROR_CODE.equals(exception.getErrorCode())) {
                return;
            }
            throw new OssStorageException(OssOperationEnum.DELETE_OBJECT.name(), objectKey, exception);
        } catch (ClientException exception) {
            throw new OssStorageException(OssOperationEnum.DELETE_OBJECT.name(), objectKey, exception);
        }
    }

    private List<OssUploadedPart> validateAndSortParts(List<OssUploadedPart> parts, long expectedFileSize) {
        Set<Integer> partNumbers = new HashSet<>();
        long totalPartSize = 0L;

        for (OssUploadedPart part : parts) {

            if (part.partNumber() < MIN_PART_NUMBER || part.partNumber() > MAX_PART_NUMBER) {
                throw new IllegalArgumentException("partNumber必须在1到10000之间");
            }

            if (!partNumbers.add(part.partNumber())) {
                throw new IllegalArgumentException("partNumber不能重复：" + part.partNumber());
            }

            if (part.etag() == null || part.etag().isBlank()) {
                throw new IllegalArgumentException("Part ETag不能为空，partNumber=" + part.partNumber());
            }

            if (part.partSize() <= 0 || part.partSize() > imageUploadProperties.getChunkSize()) {
                throw new IllegalArgumentException("Part大小不合法，partNumber=" + part.partNumber());
            }

            totalPartSize = Math.addExact(totalPartSize, part.partSize());
        }

        if (totalPartSize != expectedFileSize) {
            throw new IllegalArgumentException("全部Part大小之和与expectedFileSize不一致");
        }

        /*按照partNumber对分片进行排序*/
        List<OssUploadedPart> sortedParts = parts.stream()
                .sorted(Comparator.comparingInt(OssUploadedPart::partNumber))
                .toList();

        for (int index = 0; index < sortedParts.size(); index++) {
            int expectedPartNumber = index + 1;
            int actualPartNumber = sortedParts.get(index).partNumber();

            if (actualPartNumber != expectedPartNumber) {
                throw new IllegalArgumentException("Part编号不连续，期望" + expectedPartNumber + "，实际" + actualPartNumber);
            }
        }

        return sortedParts;
    }
    /*
     *   OSS已经合并成功
     *   ↓
     *   服务端在收到响应前发生网络超时
     *   ↓
     *   数据库仍然是COMPLETING
     *   ↓
     *   客户端重新请求Complete
     *   ↓
     *   OSS返回NoSuchUpload
     *   OSS返回 NoSuchUpload 不一定代表从未上传，也可能表示：
     *   这个Multipart会话已经完成，因此uploadId不再存在。
     *
     *   这时需要检查：
     *       1. objectKey 是否已经存在。
     *       2. 对象大小是否等于期望大小。
     *       如果都满足，返回：OssCompleteState.ALREADY_COMPLETED
     *   后续业务层还会重新计算完整SHA-256，所以这里不会只靠文件大小最终确认内容。
     * */
    private OssCompleteResult recoverCompletedObject(String objectKey, long expectedFileSize, OSSException noSuchUploadException) {
        final boolean exists;

        try {
            exists = ossClient.doesObjectExist(bucketName, objectKey);
        } catch (OSSException | ClientException exception) {
            throw new OssStorageException(OssOperationEnum.COMPLETE_MULTIPART.name(), objectKey, exception);
        }

        if (!exists) {
            throw new OssStorageException(OssOperationEnum.COMPLETE_MULTIPART.name(), objectKey, noSuchUploadException);
        }

        ObjectMetadata metadata = loadObjectMetadata(objectKey, OssOperationEnum.COMPLETE_MULTIPART);

        validateCompletedObjectSize(objectKey, expectedFileSize, metadata.getContentLength());

        return new OssCompleteResult(
                OssCompleteState.ALREADY_COMPLETED,
                metadata.getETag(),
                metadata.getContentLength()
        );
    }

    /*检查最终对象大小*/
    private void validateCompletedObjectSize(String objectKey, long expectedFileSize, long actualFileSize) {
        if (actualFileSize != expectedFileSize) {
            throw new OssStorageException(
                    OssOperationEnum.COMPLETE_MULTIPART.name(),
                    objectKey,
                    new IllegalStateException(
                            "OSS合并后的对象大小不一致，expected="
                                    + expectedFileSize + "，actual=" + actualFileSize
                    )
            );
        }
    }

    /*读取聚合后的OSS对象元信息*/
    private ObjectMetadata loadObjectMetadata(String objectKey, OssOperationEnum operation) {
        final ObjectMetadata metadata;

        try {
            metadata = ossClient.getObjectMetadata(bucketName, objectKey);
        } catch (OSSException | ClientException exception) {
            throw new OssStorageException(operation.name(), objectKey, exception);
        }

        if (metadata == null) {
            throw new OssStorageException(operation.name(), objectKey, new IllegalStateException("OSS没有返回对象元信息"));
        }

        return metadata;
    }
}
