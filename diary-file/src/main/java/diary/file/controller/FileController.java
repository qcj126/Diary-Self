package diary.file.controller;

import diary.common.entity.image.dto.ImageDTO;
import diary.common.entity.image.vo.ImageVO;
import diary.common.result.ApiResponse;
import diary.file.service.VideoFileService;
import diary.file.service.asyncservice.AsyncService;
import diary.file.service.deleteservice.DeleteService;
import diary.file.service.downloadservice.DownloadService;
import diary.file.service.queryurlservice.QueryUrlService;
import diary.file.service.uploadservice.UploadService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/file")
public class FileController {
    @Resource
    private UploadService uploadService;

    @Resource
    private DownloadService downloadService;

    @Resource
    private VideoFileService videoFileService;

    @Resource
    private QueryUrlService queryUrlService;

    @Resource
    private DeleteService deleteService;

    @PostMapping("/upload/images")
    public ApiResponse<List<Long>> upload(@RequestParam("files") List<MultipartFile> files,
                                          @RequestParam("code") Integer code) {
        ImageDTO imageDTO = new ImageDTO();
        imageDTO.setCode(code);
        imageDTO.setUserId(10000L);
        // 直接先插入数据
        List<Long> result = uploadService.addImagesToDb(files, imageDTO);

        return ApiResponse.success(result);
    }

    @PostMapping("/query/images/urls")
    public ApiResponse<List<ImageVO>> queryImageUrls(@RequestBody List<Long> imageIds) {
        // 根据 imageIds 查询图片，并动态生成签名 URL（有效期5分钟）
        // 前端每次加载日记时都应调用此接口获取最新的签名 URL
        List<ImageVO> result = queryUrlService.queryImageUrls(imageIds);
        return ApiResponse.success(result);
    }

    @PostMapping("/query/images/carousel")
    public ApiResponse<List<ImageVO>> queryCarouselImages() {
        // 查询轮播图图片  规则：查看每个分类的最新2张图片
        List<ImageVO> result = queryUrlService.queryCarouselImages();
        return ApiResponse.success(result);
    }

    @PostMapping("/download/image")
    public ApiResponse download(@RequestParam("ossUrls") List<String> ossUrls) {
        // 批量下载图片
        Map<String, Object> result = downloadService.batchDownloadPhotos(ossUrls);
        return ApiResponse.success(result);
    }

    @PostMapping("/upload/video")
    public ApiResponse uploadVideo(@RequestParam("file") MultipartFile file) {
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
