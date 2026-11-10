package diary.file.impl.asyncserviceImpl;

import com.aliyun.oss.OSS;
import diary.file.entity.dto.ImageDTO;
import diary.file.enums.ImageTransitionResultEnum;
import diary.file.property.FileProperties;
import diary.file.entity.po.ImagePO;
import diary.common.exception.ParamIllegalException;
import diary.file.mapper.ImageMapper;
import diary.file.service.ImageStatusService;
import diary.file.service.filetransactionalservice.FileTransactionalService;
import diary.utils.OSS.OssUtil;
import diary.file.service.asyncservice.AsyncService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AsyncServiceImpl implements AsyncService {
    @Resource
    private ImageStatusService imageStatusService;

    @Resource
    private OSS diaryOssClient;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Resource
    private OssUtil ossUtil;

    @Value("${download.timeout:300000}")
    private int timeout;

    @Resource
    private ImageMapper imageMapper;

    @Resource
    private FileTransactionalService fileTransactionService;

    @Async("ossUploadExecutor")
    @Override
    public void uploadAndSendMsgAsync(List<Long> result, List<File> files, List<String> originalNames, ImageDTO imageDTO) {
        if (imageDTO.getCode() == null) {
            throw new ParamIllegalException("code 不能为空");
        }

        List<String> failedFiles = new ArrayList<>();
        Map<Long, ImagePO> imageMap = imageMapper.selectImagesForUploadByIds(result).stream()
                .collect(Collectors.toMap(ImagePO::getId, Function.identity()));
        int uploadCount = Math.min(result.size(), files.size());

        // 遍历文件列表，逐个上传
        for (int i = 0; i < uploadCount; i++) {
            File file = files.get(i);
            String originalName = (i < originalNames.size()) ? originalNames.get(i) : file.getName();
            Long imageId = result.get(i);
            ImagePO imagePO = imageMap.get(imageId);
            if (imagePO == null) {
                failedFiles.add(originalName + ": 图片记录不存在");
                continue;
            }
            if (imagePO.getObjectKey() == null) {
                // 让此图片永久进入failed，等待后续人工处理
                try {
                    ImageTransitionResultEnum transitionResultEnum = imageStatusService.markUploadFailed(imageId);
                    if (transitionResultEnum == ImageTransitionResultEnum.REJECTED) {
                        log.warn("ObjectKey不存在但状态拒绝迁移，imageId={}", imageId);
                        failedFiles.add(originalName + ": ObjectKey不存在但状态拒绝迁移");
                        continue;
                    }
                } catch (Exception e) {
                    log.warn("修改状态为failed失败，名称为：{}, ID为：{}，失败原因为：", originalName, imageId, e);
                    failedFiles.add(originalName + ": ObjectKey不存在但状态拒绝迁移");
                    continue;
                }
                continue;
            }
            uploadSingleImage(imageId, file, originalName, imagePO, failedFiles);
        }
        if (!failedFiles.isEmpty()) {
            log.warn("异步上传失败列表: {}", failedFiles);
        }
    }

    private void uploadSingleImage(Long imageId, File file, String originalName, ImagePO imagePO, List<String> failedFiles) {
        // 获取图片当前状态，判断是否允许上传
        // 现在是分布式系统，所以上传前必须查看最新一次图片状态，防止其他实例已更改状态
        // 更新图片状态为UPLOADING
        try {
            ImageTransitionResultEnum transitionResultEnum = imageStatusService.markUploading(imageId);
            if (transitionResultEnum != ImageTransitionResultEnum.APPLIED) {
                log.warn("未获得此图片的上传执行权，名称为：{}, ID为：{}", originalName, imageId);
                return;
            }
        } catch (Exception e) {
            log.warn("未获得此图片的上传执行权，名称为：{}, ID为：{}，失败原因为：", originalName, imageId, e);
            failedFiles.add(originalName + ": 未获得此图片的上传执行权");
            return;
        }

        // 开始上传阿里云oss
        String fileName = imagePO.getObjectKey();
        // 1. 上传文件到 OSS（V4 客户端会自动使用 V4 签名）
        try {
            diaryOssClient.putObject(bucketName, fileName, file);
        } catch (Exception fileUploadException) {
            log.warn("图片上传OSS失败，名称为：{}, ID为：{}，失败原因为：", originalName, imageId, fileUploadException);
            // 这是可重试的失败状态，需要将状态流转为RETRY_WAIT
            try {
                ImageTransitionResultEnum transitionResultEnum = imageStatusService.markRetryWait(imageId);
                if (transitionResultEnum == ImageTransitionResultEnum.REJECTED) {
                    log.warn("OSS上传失败，且状态拒绝进入RETRY_WAIT，imageId={}", imageId);
                }
            } catch (Exception statusException) {
                log.warn("图片上传OSS失败后，状态流转为RETRY_WAIT持续失败，名称为：{}, ID为：{}，失败原因为：{}",
                        originalName, imageId, fileUploadException, statusException);
                failedFiles.add(originalName + ": 图片上传OSS失败后，状态流转为RETRY_WAIT持续失败");
                return;
            }
            return;
        }

        // 上传成功后更新状态为UPLOADED
        try {
            fileTransactionService.recordImageUploaded(imageId, fileName, originalName, failedFiles);
        } catch (Exception e) {
            log.warn("OSS成功，但状态拒绝迁移或outbox数据插入失败，名称为：{}, ID为：{}，失败原因为：", originalName, imageId, e);
            failedFiles.add(originalName + ": OSS成功，但状态拒绝迁移或outbox数据插入失败");
        }
    }

    @Async("ossDownloadExecutor")
    @Override
    public CompletableFuture<Map<String, Object>> downloadImageAsync(Long imageId, String url, String savePath) {
        Map<String, Object> result = new HashMap<>();
        result.put("ossUrl", url);
        result.put("imageId", imageId);
        try {
            // 生成签名URL（有效期5分钟）
            String signedUrl = ossUtil.generateSignedUrlByKey(url);
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
            log.error("图片下载失败, URL: {}", url, e);
            result.put("status", "failed");
            result.put("message", "下载失败: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(result);
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
