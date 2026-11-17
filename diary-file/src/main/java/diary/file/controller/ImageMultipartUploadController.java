package diary.file.controller;

import diary.common.auth.CurrentUser;
import diary.common.result.ApiResponse;
import diary.file.entity.dto.ImageUploadCompleteReqDto;
import diary.file.entity.dto.ImageUploadInitReqDto;
import diary.file.entity.vo.ImageUploadCompleteVO;
import diary.file.entity.vo.ImageUploadConfigVO;
import diary.file.entity.vo.ImageUploadInitVO;
import diary.file.service.uploadservice.ImageChunkUploadService;
import diary.file.service.uploadservice.ImageUploadCompleteService;
import diary.file.service.uploadservice.ImageUploadInitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file/upload/images")
@RequiredArgsConstructor
public class ImageMultipartUploadController {
    private final ImageUploadInitService uploadInitService;
    private final CurrentUser currentUser;
    private final ImageChunkUploadService chunkUploadService;
    private final ImageUploadCompleteService uploadCompleteService;

    @PostMapping("/init")
    public ApiResponse<ImageUploadInitVO> initializeUpload(@RequestBody ImageUploadInitReqDto request) {
        Long userId = currentUser.getUser();

        return ApiResponse.success(uploadInitService.initialize(request, userId));
    }

    @GetMapping("/config")
    public ApiResponse<ImageUploadConfigVO> getUploadConfig() {
        return ApiResponse.success(uploadInitService.getUploadConfig());
    }

    @PostMapping(value = "/{uploadId}/chunks/{chunkIndex}")
    public ApiResponse<String> uploadChunk(
            @PathVariable String uploadId,
            @PathVariable int chunkIndex,
            @RequestPart("chunk") MultipartFile chunk,
            @RequestParam("chunkSha256") String chunkSha256) {
        Long userId = currentUser.getUser();
        chunkUploadService.uploadChunk(uploadId, chunkIndex, chunk, chunkSha256, userId);

        return ApiResponse.success("分片上传成功");
    }

    @PostMapping(value = "/{uploadId}/complete")
    public ApiResponse<ImageUploadCompleteVO> completeUpload(
            @PathVariable String uploadId,
            @RequestBody ImageUploadCompleteReqDto request
    ) {
        Long userId = currentUser.getUser();

        return ApiResponse.success(uploadCompleteService.completeUpload(uploadId, request, userId));
    }
}
