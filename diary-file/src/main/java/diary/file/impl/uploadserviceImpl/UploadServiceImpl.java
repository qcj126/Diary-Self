package diary.file.impl.uploadserviceImpl;

import diary.common.entity.file.po.Photo;
import diary.config.consts.FileTypeConst;
import diary.config.consts.PhotoStatusConst;
import diary.config.consts.PhotoTypeConst;
import diary.file.mapper.PhotoMapper;
import diary.file.service.RedisService;
import diary.file.service.uploadservice.UploadService;

import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static diary.utils.commonutil.MyUtils.isEmpty;
import static diary.utils.commonutil.MyUtils.isFileEmpty;

@Service
@Slf4j
public class UploadServiceImpl implements UploadService {
    @Resource
    private PhotoMapper photoMapper;

    @Resource
    private RedisService redisService;

    @Override
    public Map<String, Object> addPhotosToDb(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Map.of("code", 500, "message", "文件列表为空", "data", "null");
        }

        List<Photo> photoList = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        // 第一步：验证所有文件并构建Photo对象列表
        for (MultipartFile file : files) {
            try {
                if (isFileEmpty(file)) {
                    failedFiles.add(file.getOriginalFilename() + ": 文件为空");
                    continue;
                }

                // 验证是否为图片类型
                String photoFormat = file.getContentType();
                if (isEmpty(photoFormat) || !photoFormat.startsWith("image")) {
                    try {
                        if (ImageIO.read(file.getInputStream()) == null) {
                            failedFiles.add(file.getOriginalFilename() + ": 文件不是图片类型");
                        }
                    } catch (Exception e) {
                        failedFiles.add(file.getOriginalFilename() + ": 文件读取失败");
                    }
                    continue;
                }

                long id = MyUtils.getPrimaryKey();
                String photoType = PhotoTypeConst.PHOTO_TYPE_SWEETY;
                String photoName = file.getOriginalFilename();

                // 查看同一图片所属类别下是否有相同名称的图片
                Integer isExist = photoMapper.selectPhotoByTypeAndName(photoType, photoName);
                if (isExist > 0) {
                    failedFiles.add(photoName + ": 图片已存在");
                    continue;
                }

                long photoSize = file.getSize();
                String photoStatus = PhotoStatusConst.PHOTO_STATUS_PROCESSING;

                // 构建Photo对象（暂不设置sortOrder）
                Photo photo = new Photo();
                photo.setId(id);
                photo.setPhotoType(photoType);
                photo.setPhotoName(photoName);
                photo.setPhotoSize(photoSize);
                photo.setPhotoFormat(photoFormat);
                photo.setPhotoStatus(photoStatus);

                photoList.add(photo);
            } catch (Exception e) {
                log.error("处理文件 {} 时发生异常", file.getOriginalFilename(), e);
                failedFiles.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        // 第二步：分批插入数据库，每批最多20条
        List<Long> photoIds = new ArrayList<>();
        int batchSize = 20;
        int totalSize = photoList.size();

        for (int i = 0; i < totalSize; i += batchSize) {
            // 计算当前批次的结束位置
            int end = Math.min(i + batchSize, totalSize);
            List<Photo> batchList = photoList.subList(i, end);

            try {
                // 获取当前Redis中的图片数量，作为本批次起始序号
                long currentCount = redisService.getPhotoCount();

                // 为本批次的Photo设置连续的sortOrder
                for (int j = 0; j < batchList.size(); j++) {
                    batchList.get(j).setSortOrder(currentCount + j + 1);
                }

                Integer count = photoMapper.batchAddPhotoToDb(batchList);
                if (count != null && count > 0) {
                    // 一次性更新Redis：当前数量 + 本批次插入数量
                    redisService.updatePhotoCount(currentCount + batchList.size());

                    // 收集成功插入的id
                    for (Photo photo : batchList) {
                        photoIds.add(photo.getId());
                    }
                    log.info("批量插入照片成功，批次范围: {} - {}，插入数量: {}，sortOrder范围: {} - {}",
                            i + 1, end, count, currentCount + 1, currentCount + batchList.size());
                } else {
                    // 记录失败的文件
                    for (Photo photo : batchList) {
                        failedFiles.add(photo.getPhotoName() + ": 批量插入失败");
                    }
                    log.error("批量插入照片失败，批次范围: {} - {}", i + 1, end);
                }
            } catch (Exception e) {
                log.error("批量插入照片异常，批次范围: {} - {}", i + 1, end, e);
                // 记录失败的文件
                for (Photo photo : batchList) {
                    failedFiles.add(photo.getPhotoName() + ": " + e.getMessage());
                }
            }
        }

        if (photoIds.isEmpty()) {
            return Map.of("code", 500, "message", "所有文件处理失败", "data", "null", "failedFiles", failedFiles);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", photoIds);
        if (!failedFiles.isEmpty()) {
            result.put("failedFiles", failedFiles);
            result.put("message", "部分文件处理成功");
        }
        return result;
    }

    @Override
    public Map<String, Object> addVideoToDb(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Map.of("code", 500, "message", "文件为空", "data", "null");
        }

        try {
            // 验证是否为视频类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith(FileTypeConst.CONTENT_TYPE_VIDEO_PREFIX)) {
                // 进一步检查文件扩展名
                String originalFilename = file.getOriginalFilename();
                if (!isVideoFileByExtension(originalFilename)) {
                    return Map.of("code", 500, "message", "文件不是视频类型", "data", "null");
                }
            }

            long id = MyUtils.getPrimaryKey();
            String videoType = PhotoTypeConst.VIDEO_TYPE;
            String videoName = file.getOriginalFilename();

            // 查看同一视频类别下是否有相同名称的视频
            Integer isExist = photoMapper.selectPhotoByTypeAndName(videoType, videoName);
            if (isExist > 0) {
                return Map.of("code", 500, "message", "视频已存在", "data", "null");
            }

            long videoSize = file.getSize();
            String videoFormat = contentType != null ? contentType : getFileExtension(videoName);
            String videoStatus = PhotoStatusConst.PHOTO_STATUS_PROCESSING;

            // 获取当前Redis中的视频数量，作为sortOrder
            long currentCount = redisService.getPhotoCount();

            // 插入数据库
            Integer count = photoMapper.addPhotoToDb(id, videoType, videoName, videoSize, videoFormat, currentCount + 1, videoStatus);
            if (count != null && count > 0) {
                // 更新Redis计数
                redisService.updatePhotoCount(currentCount + 1);
                log.info("视频信息插入数据库成功，videoId: {}, videoName: {}", id, videoName);
                return Map.of("code", 200, "message", "视频信息保存成功", "data", id);
            } else {
                log.error("视频信息插入数据库失败，videoName: {}", videoName);
                return Map.of("code", 500, "message", "视频信息保存失败", "data", "null");
            }
        } catch (Exception e) {
            log.error("处理视频文件时发生异常，文件名: {}", file.getOriginalFilename(), e);
            return Map.of("code", 500, "message", "处理视频文件异常: " + e.getMessage(), "data", "null");
        }
    }

    /**
     * 根据文件扩展名判断是否为视频文件
     */
    private boolean isVideoFileByExtension(String fileName) {
        if (fileName == null) return false;
        String extension = getFileExtension(fileName).toLowerCase();
        return FileTypeConst.VIDEO_EXTENSIONS.contains(extension);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf(".");
        return dotIndex > 0 && dotIndex < fileName.length() - 1
                ? fileName.substring(dotIndex + 1) : "";
    }
}