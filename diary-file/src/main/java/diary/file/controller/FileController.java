package diary.file.controller;

import diary.common.entity.file.dto.IconDTO;
import diary.common.entity.image.vo.ImageVO;
import diary.common.result.ApiResponse;
import diary.file.service.VideoFileService;
import diary.file.service.addservice.IconAddService;
import diary.file.service.deleteservice.DeleteService;
import diary.file.service.deleteservice.IconDeleteService;
import diary.file.service.downloadservice.DownloadService;
import diary.file.service.queryservice.IconQueryService;
import diary.file.service.queryurlservice.QueryUrlService;
import diary.file.service.updateservice.IconUpdateService;
import diary.file.service.uploadservice.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {
    private static final Path ICON_DIR = Paths.get("E:\\Diary-Front\\icon").normalize();

    private final UploadService uploadService;

    private final DownloadService downloadService;

    private final VideoFileService videoFileService;

    private final QueryUrlService queryUrlService;

    private final DeleteService deleteService;

    private final IconAddService iconAddService;

    private final IconDeleteService iconDeleteService;

    private final IconQueryService iconQueryService;

    private final IconUpdateService iconUpdateService;

    @PostMapping("/upload/images")
    public ApiResponse<List<Long>> upload(@RequestParam("files") List<MultipartFile> files,
                                          @RequestParam("code") Integer code) {
        return ApiResponse.success(uploadService.uploadImagesAndInsert(files, code));
    }

    @PostMapping("/query/images/urls")
    public ApiResponse<List<ImageVO>> queryImageUrls(@RequestBody List<Long> imageIds) {
        // 根据 imageIds 查询图片，并动态生成签名 URL（有效期5分钟）
        // 前端每次加载日记时都应调用此接口获取最新的签名 URL
        return ApiResponse.success(queryUrlService.queryImageUrls(imageIds));
    }

    @PostMapping("/query/images/carousel")
    public ApiResponse<List<ImageVO>> queryCarouselImages() {
        // 查询轮播图图片  规则：查看每个分类的最新2张图片
        return ApiResponse.success(queryUrlService.queryCarouselImages());
    }

    @PostMapping("/download/image")
    public ApiResponse<Map<String, Object>> download(@RequestBody Map<Long, String> imageIdUrls) {
        // 批量下载图片
        return ApiResponse.success(downloadService.batchDownloadPhotos(imageIdUrls));
    }

    @PostMapping("/upload/video")
    public ApiResponse<Map<String, Object>> uploadVideo(@RequestParam("file") MultipartFile file) {
        // 直接先插入数据
        Map<String, Object> result = videoFileService.addVideoToDb(file);
        // 异步上传视频到OSS成功后，发送消息给mq
        videoFileService.uploadAndSendMsgAsync(result, file);
        return ApiResponse.success(result);
    }

    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteFile(@PathVariable Long id) {
        // Implementation for deleting a file
        return ApiResponse.success(deleteService.deleteImage(id));
    }

    @PostMapping("/icon/add")
    public ApiResponse<?> addIcon(@RequestParam("file") MultipartFile file,
                                  @ModelAttribute IconDTO iconDTO) {
        return iconAddService.addIcon(file, iconDTO);
    }

    @PostMapping("/icon/query")
    public ApiResponse<?> queryIcons(@RequestBody(required = false) IconDTO iconDTO) {
        return iconQueryService.queryIcons(iconDTO);
    }

    @PostMapping("/icon/update")
    public ApiResponse<?> updateIcon(@RequestParam(value = "file", required = false) MultipartFile file,
                                     @ModelAttribute IconDTO iconDTO) {
        return iconUpdateService.updateIcon(file, iconDTO);
    }

    @PostMapping("/icon/delete")
    public ApiResponse<?> deleteIcon(@RequestBody IconDTO iconDTO) {
        return iconDeleteService.deleteIcon(iconDTO);
    }

    @GetMapping(value = "/icon/{fileName}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getIcon(@PathVariable String fileName) throws IOException {
        Path iconPath = resolveIconPath(fileName);
        if (iconPath == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(Files.readAllBytes(iconPath));
    }

    private Path resolveIconPath(String fileName) {
        String decodedFileName = UriUtils.decode(fileName, StandardCharsets.UTF_8);
        Path iconPath = ICON_DIR.resolve(decodedFileName).normalize();
        if (!iconPath.startsWith(ICON_DIR) || !Files.isRegularFile(iconPath)) {
            return null;
        }
        return iconPath;
    }
}
