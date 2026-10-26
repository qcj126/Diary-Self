package diary.file.controller;

import diary.file.entity.vo.ImageVO;
import diary.common.auth.CurrentUser;
import diary.file.service.ImageFileService;
import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import diary.file.service.VideoFileService;
import diary.file.service.deleteservice.DeleteService;
import diary.file.service.downloadservice.DownloadService;
import diary.file.service.queryurlservice.QueryUrlService;
import diary.file.service.uploadservice.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final UploadService uploadService;

    private final DownloadService downloadService;

    private final VideoFileService videoFileService;

    private final QueryUrlService queryUrlService;

    private final DeleteService deleteService;

    private final ImageFileService imageFileService;

    private final CurrentUser currentUser;

    @OperLog(module = "文件管理", description = "批量上传图片", operationType = "UPLOAD", saveParams = false, saveResult = true)
    @PostMapping("/upload/images")
    public ApiResponse<List<Long>> upload(@RequestParam("files") List<MultipartFile> files,
                                          @RequestParam("code") Integer code) {
        return ApiResponse.success(uploadService.uploadImagesAndInsert(files, code, currentUser.getUser()));
    }

    @OperLog(module = "文件管理", description = "查询图片访问地址", operationType = "SELECT", saveResult = false)
    @PostMapping("/query/images/urls")
    public ApiResponse<List<ImageVO>> queryImageUrls(@RequestBody List<Long> imageIds) {
        return ApiResponse.success(queryUrlService.queryImageUrls(imageIds));
    }

    /**
     * 与 /file/sys/icon/{fileName} 一致，按原始文件名读取图片。
     * 本地不存在时由 ImageFileService 从 OSS 回源并写回本地。
     */
    @OperLog(module = "文件管理", description = "获取图片文件", operationType = "DOWNLOAD", saveResult = false)
    @GetMapping("/images/{fileName:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String fileName) {
        String originalName = UriUtils.decode(fileName, StandardCharsets.UTF_8);
        Path imagePath = imageFileService.getOrRestore(originalName);
        if (imagePath == null || !Files.isRegularFile(imagePath)) {
            return ResponseEntity.notFound().build();
        }
        try {
            String contentType = Files.probeContentType(imagePath);
            MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
            return ResponseEntity.ok().contentType(mediaType).body(Files.readAllBytes(imagePath));
        } catch (IOException | IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @OperLog(module = "文件管理", description = "查询轮播图片", operationType = "SELECT", saveParams = false, saveResult = false)
    @PostMapping("/query/images/carousel")
    public ApiResponse<List<ImageVO>> queryCarouselImages() {
        // 查询轮播图图片  规则：查看每个分类的最新2张图片
        return ApiResponse.success(queryUrlService.queryCarouselImages());
    }

    @OperLog(module = "文件管理", description = "批量下载图片", operationType = "DOWNLOAD", saveParams = false, saveResult = false)
    @PostMapping("/download/image")
    public ApiResponse<Map<String, Object>> download(@RequestBody Map<Long, String> imageIdUrls) {
        // 批量下载图片
        return ApiResponse.success(downloadService.batchDownloadPhotos(imageIdUrls));
    }

    @OperLog(module = "文件管理", description = "上传视频", operationType = "UPLOAD", saveParams = false, saveResult = false)
    @PostMapping("/upload/video")
    public ApiResponse<Map<String, Object>> uploadVideo(@RequestParam("file") MultipartFile file) {
        // 直接先插入数据
        Map<String, Object> result = videoFileService.addVideoToDb(file);
        // 异步上传视频到OSS成功后，发送消息给mq
        videoFileService.uploadAndSendMsgAsync(result, file);
        return ApiResponse.success(result);
    }

    @OperLog(module = "文件管理", description = "删除图片文件", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteFile(@PathVariable Long id) {
        // Implementation for deleting a file
        return ApiResponse.success(deleteService.deleteImage(id));
    }
}
