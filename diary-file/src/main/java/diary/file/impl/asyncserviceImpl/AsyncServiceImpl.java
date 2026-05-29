package diary.file.impl.asyncserviceImpl;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import diary.common.entity.file.po.OssUploadSuccessMsg;
import diary.common.enums.typeenum.TypeEnum;
import diary.config.mqconfig.RabbitMqConfig;
import diary.utils.OSS.OssUtil;
import diary.file.service.asyncservice.AsyncService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AsyncServiceImpl implements AsyncService {
    // 分片大小：10MB
    private static final long PART_SIZE = 10 * 1024 * 1024L;

    // 大文件阈值：100MB
    private static final long LARGE_FILE_THRESHOLD = 100 * 1024 * 1024L;

    @Resource
    private OSS ossClient;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private OssUtil ossUtil;

    @Value("${download.timeout:300000}")
    private int timeout;

    @Async("ossUploadExecutor")
    @Override
    public void uploadAndSendMsgAsync(Map<String, Object> result, List<MultipartFile> files, Integer code) {
        // 获取photoId列表
        Object dataObj = result.get("data");
        if (dataObj == null) {
            log.error("数据库插入失败，result: {}", result);
            throw new RuntimeException("数据插入DB失败");
        }

        List<Long> photoIds;
        if (dataObj instanceof Long) {
            // 兼容单个ID的情况
            photoIds = List.of((Long) dataObj);
        } else if (dataObj instanceof List) {
            photoIds = (List<Long>) dataObj;
        } else {
            log.error("未知的数据类型: {}", dataObj.getClass());
            throw new RuntimeException("数据格式错误");
        }

        if (photoIds.size() != files.size()) {
            log.warn("photoId数量({})与文件数量({})不匹配", photoIds.size(), files.size());
        }

        int successCount = 0;
        int failCount = 0;
        List<String> failedFiles = new ArrayList<>();

        // 遍历文件列表，逐个上传
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            Long photoId = (i < photoIds.size()) ? photoIds.get(i) : null;

            if (photoId == null) {
                log.warn("文件 {} 没有对应的photoId，跳过上传", file.getOriginalFilename());
                failCount++;
                failedFiles.add(file.getOriginalFilename() + ": 缺少photoId");
                continue;
            }

            try {
                // 生成唯一文件名，避免同名覆盖
                String type = TypeEnum.getType(code);
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

                // 1. 上传文件到 OSS（V4 客户端会自动使用 V4 签名）
                ossClient.putObject(bucketName, fileName, file.getInputStream());

                // 2. 生成 V4 预签名 URL，有效期设为 1 小时
                Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);
                GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, fileName);
                request.setExpiration(expiration);
                request.setMethod(HttpMethod.GET);
                String ossUrl = ossClient.generatePresignedUrl(request).toString();

                // 构建消息对象
                OssUploadSuccessMsg msg = new OssUploadSuccessMsg(
                        photoId, ossUrl, file.getOriginalFilename(), System.currentTimeMillis()
                );

                // 发送消息到rabbitmq
                // 创建关联ID
                String correlationId = "UPLOAD" + photoId + System.currentTimeMillis();
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.OSS_UPLOAD_EXCHANGE,
                        RabbitMqConfig.OSS_UPLOAD_ROUTING_KEY,
                        msg,
                        new CorrelationData(correlationId)
                );
                log.info("OSS 上传成功，消息已发送，photoId: {}, fileName: {}, correlationId: {}",
                        photoId, file.getOriginalFilename(), correlationId);
                successCount++;
            } catch (IOException e) {
                log.error("OSS 上传失败，photoId: {}, fileName: {}", photoId, file.getOriginalFilename(), e);
                failCount++;
                failedFiles.add(file.getOriginalFilename() + ": " + e.getMessage());
                // 可在此发送上传失败消息到另一个队列
            } catch (Exception e) {
                log.error("处理文件异常，photoId: {}, fileName: {}", photoId, file.getOriginalFilename(), e);
                failCount++;
                failedFiles.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        log.info("批量上传完成，成功: {}, 失败: {}", successCount, failCount);
        if (!failedFiles.isEmpty()) {
            log.warn("失败文件列表: {}", failedFiles);
        }
    }

    @Async("ossDownloadExecutor")
    @Override
    public CompletableFuture<Map<String, Object>> downloadImageAsync(String ossUrl, String savePath) {
        Map<String, Object> result = new HashMap<>();
        result.put("ossUrl", ossUrl);

        try {
            // 生成签名URL（有效期5分钟）
            String signedUrl = ossUtil.generateSignedUrlByKey(ossUrl);
            log.info("生成签名URL: {}", signedUrl);

            // 从签名URL中提取文件名
            String fileName = extractFileNameFromUrl(signedUrl);
            if (fileName == null || fileName.isEmpty()) {
                fileName = "image_" + System.currentTimeMillis() + ".jpg";
            }

            // 构建完整保存路径
            Path fullPath = Paths.get(savePath, fileName);

            // 如果文件已存在，添加时间戳避免覆盖
            if (Files.exists(fullPath)) {
                String nameWithoutExt = getFileNameWithoutExtension(fileName);
                String extension = getFileExtension(fileName);
                String newFileName = nameWithoutExt + "_" + System.currentTimeMillis() + "." + extension;
                fullPath = Paths.get(savePath, newFileName);
            }

            log.info("开始下载图片: {} -> {}", signedUrl, fullPath);

            // 使用HttpURLConnection下载文件，超时时间5分钟
            downloadFileWithHttpURLConnection(signedUrl, fullPath.toFile());

            result.put("status", "success");
            result.put("filePath", fullPath.toString());
            result.put("fileName", fullPath.getFileName().toString());
            result.put("message", "下载成功");

            log.info("图片下载成功: {}", fullPath);

        } catch (Exception e) {
            log.error("图片下载失败, URL: {}", ossUrl, e);
            result.put("status", "failed");
            result.put("message", "下载失败: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(result);
    }

    @Async("ossUploadExecutor")
    @Override
    public void uploadAndSendMsgAsync(Map<String, Object> result, MultipartFile file) {
        // 获取videoId
        Object dataObj = result.get("data");
        if (dataObj == null) {
            log.error("数据库插入失败，result: {}", result);
            throw new RuntimeException("数据插入DB失败");
        }

        Long videoId;
        if (dataObj instanceof Long) {
            videoId = (Long) dataObj;
        } else {
            log.error("未知的数据类型: {}", dataObj.getClass());
            throw new RuntimeException("数据格式错误");
        }

        try {
            // 生成唯一文件名，避免同名覆盖
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            String ossUrl;
            // 根据文件大小选择上传方式
            if (file.getSize() > LARGE_FILE_THRESHOLD) {
                // 大文件使用分片上传
                ossUrl = multipartUpload(file, fileName);
            } else {
                // 小文件使用普通上传
                ossUrl = simpleUpload(file, fileName);
            }

            // 构建消息对象
            OssUploadSuccessMsg msg = new OssUploadSuccessMsg(
                    videoId, ossUrl, file.getOriginalFilename(), System.currentTimeMillis()
            );

            // 发送消息到rabbitmq
            String correlationId = "VIDEO_UPLOAD" + videoId + System.currentTimeMillis();
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.OSS_UPLOAD_EXCHANGE,
                    RabbitMqConfig.OSS_UPLOAD_ROUTING_KEY,
                    msg,
                    new CorrelationData(correlationId)
            );
            log.info("视频OSS上传成功，消息已发送，videoId: {}, fileName: {}, correlationId: {}",
                    videoId, file.getOriginalFilename(), correlationId);
        } catch (IOException e) {
            log.error("视频OSS上传失败，videoId: {}, fileName: {}", videoId, file.getOriginalFilename(), e);
            // 可在此发送上传失败消息到另一个队列
        } catch (Exception e) {
            log.error("处理视频文件异常，videoId: {}, fileName: {}", videoId, file.getOriginalFilename(), e);
        }
    }

    /**
     * 简单上传（适用于小文件）
     */
    private String simpleUpload(MultipartFile file, String fileName) throws IOException {
        // 上传文件到OSS
        ossClient.putObject(bucketName, fileName, file.getInputStream());

        // 生成预签名URL，有效期1小时
        Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, fileName);
        request.setExpiration(expiration);
        request.setMethod(com.aliyun.oss.HttpMethod.GET);
        return ossClient.generatePresignedUrl(request).toString();
    }

    /**
     * 分片上传（适用于大文件，支持断点续传）
     */
    private String multipartUpload(MultipartFile file, String fileName) throws IOException {
        String uploadId = null;
        try {
            // 1. 初始化分片上传
            InitiateMultipartUploadRequest initiateRequest = new InitiateMultipartUploadRequest(bucketName, fileName);
            InitiateMultipartUploadResult initiateResult = ossClient.initiateMultipartUpload(initiateRequest);
            uploadId = initiateResult.getUploadId();

            // 2. 计算分片数量
            long contentLength = file.getSize();
            int partCount = (int) (contentLength / PART_SIZE);
            if (contentLength % PART_SIZE != 0) {
                partCount++;
            }

            // 3. 上传分片
            List<PartETag> partETags = new ArrayList<>();
            try (InputStream inputStream = file.getInputStream()) {
                for (int i = 0; i < partCount; i++) {
                    // 跳过已上传的分片
                    long startPos = i * PART_SIZE;
                    long curPartSize = (i + 1 == partCount) ? (contentLength - startPos) : PART_SIZE;

                    // 创建分片上传请求
                    UploadPartRequest uploadPartRequest = new UploadPartRequest();
                    uploadPartRequest.setBucketName(bucketName);
                    uploadPartRequest.setKey(fileName);
                    uploadPartRequest.setUploadId(uploadId);
                    uploadPartRequest.setInputStream(inputStream);
                    uploadPartRequest.setPartSize(curPartSize);
                    uploadPartRequest.setPartNumber(i + 1);

                    // 上传分片
                    UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                    partETags.add(uploadPartResult.getPartETag());

                    log.debug("视频分片上传成功，fileName: {}, partNumber: {}/{}", fileName, i + 1, partCount);
                }
            }

            // 4. 完成分片上传
            CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
                    bucketName, fileName, uploadId, partETags);
            ossClient.completeMultipartUpload(completeRequest);

            // 5. 生成预签名URL
            return ossUtil.getSignedUrlByFileName(fileName);
        } catch (Exception e) {
            // 如果上传失败，取消分片上传
            if (uploadId != null) {
                try {
                    AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(
                            bucketName, fileName, uploadId);
                    ossClient.abortMultipartUpload(abortRequest);
                    log.warn("取消分片上传，fileName: {}, uploadId: {}", fileName, uploadId);
                } catch (Exception abortEx) {
                    log.error("取消分片上传失败，fileName: {}, uploadId: {}", fileName, uploadId, abortEx);
                }
            }
            throw new IOException("分片上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用HttpURLConnection下载文件到本地
     *
     * @param fileUrl  文件URL（签名URL）
     * @param destFile 目标文件
     */
    private void downloadFileWithHttpURLConnection(String fileUrl, File destFile) throws IOException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            URI uri = new URI(fileUrl);
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            // 设置连接超时和读取超时
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP响应码: " + responseCode);
            }

            // 获取文件大小
            long contentLength = connection.getContentLengthLong();
            log.debug("文件大小: {} bytes", contentLength);

            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream(destFile);

            byte[] buffer = new byte[8192]; // 增大缓冲区到8KB
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            outputStream.flush();
            log.debug("已下载: {} bytes", totalBytesRead);

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.error("关闭输出流失败", e);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("关闭输入流失败", e);
                }
            }
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * 从URL中提取文件名
     */
    private String extractFileNameFromUrl(String url) {
        try {
            // 移除URL参数
            String urlWithoutParams = url.split("\\?")[0];
            String fileName = urlWithoutParams.substring(urlWithoutParams.lastIndexOf("/") + 1);

            // URL解码
            fileName = java.net.URLDecoder.decode(fileName, StandardCharsets.UTF_8);

            // 验证文件名是否合法
            if (fileName.contains(".")) return fileName;
        } catch (Exception e) {
            log.warn("从URL提取文件名失败: {}", url, e);
        }
        return null;
    }

    /**
     * 获取不带扩展名的文件名
     */
    private String getFileNameWithoutExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        return dotIndex > 0 && dotIndex < fileName.length() - 1
                ? fileName.substring(dotIndex + 1) : "jpg";
    }
}
