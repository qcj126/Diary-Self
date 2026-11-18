package diary.file.controller;

import diary.file.entity.vo.ImageUploadStatusVO;
import diary.file.entity.vo.ImageVO;
import diary.common.auth.CurrentUser;
import diary.file.service.OriginalImageService;
import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import diary.file.service.ImageUploadStatusQueryService;
import diary.file.service.deleteservice.DeleteService;
import diary.file.service.queryurlservice.QueryUrlService;
import diary.file.service.queryurlservice.ThumbnailQueryService;
import diary.file.entity.model.OssObjectResource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final QueryUrlService queryUrlService;

    private final DeleteService deleteService;

    private final OriginalImageService originalImageService;

    private final CurrentUser currentUser;

    private final ImageUploadStatusQueryService imageUploadStatusQueryService;

    private final ThumbnailQueryService thumbnailQueryService;

    @OperLog(module = "文件管理", description = "查询图片访问地址", operationType = "SELECT", saveResult = false)
    @PostMapping("/query/images/urls")
    public ApiResponse<List<ImageVO>> queryImageUrls(@RequestBody List<Long> imageIds) {
        return ApiResponse.success(queryUrlService.queryImageUrls(imageIds, currentUser.getUser()));
    }

    @OperLog(module = "文件管理", description = "查询缩略图访问地址", operationType = "SELECT", saveResult = false)
    @PostMapping("/query/thumbnails/urls")
    public ApiResponse<List<ImageVO>> queryThumbnailUrls(@RequestBody List<Long> imageIds) {
        return ApiResponse.success(thumbnailQueryService.queryThumbnailUrls(imageIds, currentUser.getUser()));
    }

    @OperLog(module = "文件管理", description = "获取缩略图文件", operationType = "DOWNLOAD", saveResult = false)
    @GetMapping("/thumbnails/{imageId}")
    public ResponseEntity<StreamingResponseBody> getThumbnail(@PathVariable Long imageId) {
        return stream(thumbnailQueryService.openThumbnail(imageId, currentUser.getUser()));
    }

    @OperLog(module = "文件管理", description = "批量查询图片上传状态", operationType = "SELECT", saveParams = true, saveResult = true)
    @PostMapping("/query/images/statuses")
    public ApiResponse<List<ImageUploadStatusVO>> queryImageUploadStatuses(
            @RequestBody List<Long> imageIds) {
        return ApiResponse.success(
                imageUploadStatusQueryService.queryStatuses(
                        imageIds,
                        currentUser.getUser()
                )
        );
    }

    @OperLog(module = "文件管理", description = "获取原图", operationType = "DOWNLOAD", saveResult = false)
    @GetMapping("/originals/{imageId}")
    public ResponseEntity<StreamingResponseBody> getOriginalImage(@PathVariable Long imageId) {
        return stream(originalImageService.openByImageId(imageId, currentUser.getUser()));
    }

    /** 兼容历史数据中已经保存的 /file/images/{fileName} 地址，不再写入节点本地目录。 */
    @OperLog(module = "文件管理", description = "兼容旧原图地址", operationType = "DOWNLOAD", saveResult = false)
    @GetMapping("/images/{fileName:.+}")
    public ResponseEntity<StreamingResponseBody> getImage(@PathVariable String fileName) {
        String originalName = UriUtils.decode(fileName, StandardCharsets.UTF_8);
        return stream(originalImageService.openByOriginalName(originalName, currentUser.getUser()));
    }

    @OperLog(module = "文件管理", description = "查询轮播图片", operationType = "SELECT", saveParams = false, saveResult = false)
    @PostMapping("/query/images/carousel")
    public ApiResponse<List<ImageVO>> queryCarouselImages() {
        // 查询轮播图图片  规则：查看每个分类的最新2张图片
        return ApiResponse.success(queryUrlService.queryCarouselImages(currentUser.getUser()));
    }

    @OperLog(module = "文件管理", description = "删除图片文件", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteFile(@PathVariable Long id) {
        // Implementation for deleting a.txt file
        return ApiResponse.success(deleteService.deleteImage(id, currentUser.getUser()));
    }

    private ResponseEntity<StreamingResponseBody> stream(OssObjectResource resource) {
        if (resource == null) return ResponseEntity.notFound().build();
        MediaType mediaType;
        try {
            mediaType = resource.getContentType() == null || resource.getContentType().isBlank()
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(resource.getContentType());
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        StreamingResponseBody body = outputStream -> {
            try (resource) {
                resource.getInputStream().transferTo(outputStream);
            }
        };
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().contentType(mediaType);
        if (resource.getContentLength() >= 0) response.contentLength(resource.getContentLength());
        return response.body(body);
    }
}
