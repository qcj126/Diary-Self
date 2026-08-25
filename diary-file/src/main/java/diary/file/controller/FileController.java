package diary.file.controller;

import diary.common.entity.image.vo.ImageVO;
import diary.common.result.ApiResponse;
import diary.file.service.VideoFileService;
import diary.file.service.deleteservice.DeleteService;
import diary.file.service.downloadservice.DownloadService;
import diary.file.service.queryurlservice.QueryUrlService;
import diary.file.service.uploadservice.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
}
