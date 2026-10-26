package diary.file.impl.uploadserviceImpl;

import diary.file.consts.PhotoStatusConst;
import diary.file.entity.dto.ImageDTO;
import diary.file.entity.po.ImagePO;
import diary.file.enums.TypeEnum;
import diary.common.exception.ParamIllegalException;
import diary.file.mapper.ImageMapper;
import diary.file.service.ImageFileService;
import diary.file.service.asyncservice.AsyncService;
import diary.file.service.uploadservice.UploadService;

import diary.utils.commonutil.MyUtils;
import diary.file.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {
    private final ImageMapper imagMapper;

    private final FileUtil fileUtil;

    private final ImageFileService imageFileService;

    private final AsyncService asyncService;

    @Override
    public List<Long> uploadImagesAndInsert(List<MultipartFile> files, Integer code, Long userId) {
        // 拦截参数为空的数据
        MyUtils.check().notNull(files, "文件列表").listNotContainsEmpty(files, "文件列表").notNull(code, "图片类别");

        // 过滤非图片文件
        files = files.stream()
                .filter(file -> {
                    MyUtils.check().notEmpty(file.getContentType(), "文件类型");
                    return Objects.equals(file.getContentType(), "image/png") || Objects.equals(file.getContentType(), "image/jpeg");
                }).toList();

        ImageDTO imageDTO = new ImageDTO();
        imageDTO.setUserId(userId);
        imageDTO.setCode(code);

        List<UploadTask> uploadTasks = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        List<Long> imageIds = new ArrayList<>();

        // 第一步：验证所有文件并构建Photo对象列表
        for (MultipartFile file : files) {
            try {
                Integer type = TypeEnum.getCode(imageDTO.getCode());
                String originalFilename = fileUtil.getSafeOriginalName(file.getOriginalFilename());
                String objectKey = originalFilename;

                // 查看同一图片所属类别下是否有相同名称的图片
                Long isExist = imagMapper.selectImageByTypeAndName(type, originalFilename);
                if (isExist != null) {
                    // 重复上传不再重复写 OSS；若本地缓存已被清理，则立即从 OSS 回写。
                    if (imageFileService.getOrRestore(originalFilename) != null) {
                        log.info("图片已存在，已确认本地缓存: {}", originalFilename);
                        imageIds.add(isExist);
                    } else {
                        failedFiles.add(originalFilename + ": 本地与 OSS 均未找到已存在图片");
                    }
                    continue;
                }

                // 构建Photo对象（暂不设置sortOrder）
                ImagePO image = new ImagePO();
                image.setId(MyUtils.getPrimaryKey());
                image.setUserId(imageDTO.getUserId());
                image.setFileSize(file.getSize());
                image.setOriginalName(originalFilename);
                image.setMimeType(file.getContentType());
                image.setType(type);
                image.setStatus(PhotoStatusConst.PHOTO_STATUS_PROCESSING);
                image.setObjectKey(objectKey);
                uploadTasks.add(new UploadTask(image, file));
            } catch (Exception e) {
                log.error("处理文件 {} 时发生异常", file.getOriginalFilename(), e);
                failedFiles.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        // 第二步：先将新文件落盘。之后的 OSS 上传始终读取这个本地副本。
        Map<Long, File> localFilesByImageId = new HashMap<>();
        List<UploadTask> localTasks = new ArrayList<>();
        for (UploadTask task : uploadTasks) {
            try {
                File localFile = fileUtil.saveToAppPicture(task.file(), task.image().getOriginalName());
                localFilesByImageId.put(task.image().getId(), localFile);
                localTasks.add(task);
                log.info("图片已保存到本地: {}", localFile.getAbsolutePath());
            } catch (Exception e) {
                failedFiles.add(task.image().getOriginalName() + ": 本地保存失败: " + e.getMessage());
                log.error("保存本地图片失败: {}", task.image().getOriginalName(), e);
            }
        }

        // 第三步：本地保存成功后再分批插入数据库，每批最多20条。
        int batchSize = 20;
        int totalSize = localTasks.size();
        List<UploadTask> persistedTasks = new ArrayList<>();

        for (int i = 0; i < totalSize; i += batchSize) {
            // 计算当前批次的结束位置
            int end = Math.min(i + batchSize, totalSize);
            List<UploadTask> batchTasks = localTasks.subList(i, end);
            List<ImagePO> batchList = batchTasks.stream().map(UploadTask::image).toList();

            try {
                Integer count = imagMapper.batchAddImageToDb(batchList);
                if (count != null && count > 0) {
                    // 收集成功插入的id
                    for (ImagePO image : batchList) {
                        imageIds.add(image.getId());
                    }
                    persistedTasks.addAll(batchTasks);
                } else {
                    // 记录失败的文件
                    for (ImagePO image : batchList) {
                        failedFiles.add(image.getOriginalName() + ": 批量插入失败");
                    }
                    batchTasks.forEach(task -> fileUtil.deleteFile(localFilesByImageId.get(task.image().getId()).getAbsolutePath()));
                    log.error("批量插入照片失败，批次范围: {} - {}", i + 1, end);
                }
            } catch (Exception e) {
                log.error("批量插入照片异常，批次范围: {} - {}", i + 1, end, e);
                // 记录失败的文件
                for (ImagePO image : batchList) {
                    failedFiles.add(image.getOriginalName() + ": " + e.getMessage());
                }
                batchTasks.forEach(task -> fileUtil.deleteFile(localFilesByImageId.get(task.image().getId()).getAbsolutePath()));
            }
        }

        // TODO 后续处理插入失败的情况】
        if (!failedFiles.isEmpty()) {
            log.error("处理文件列表时发生异常，失败文件列表: {}", failedFiles);
        }

        if (imageIds.isEmpty()) {
            throw new ParamIllegalException("所有文件均处理失败");
        }
        // 第四步：异步上传已落盘的本地图片到 OSS。
        List<File> localFiles = new ArrayList<>();
        List<Long> uploadImageIds = new ArrayList<>();
        List<String> originalNames = new ArrayList<>();
        for (UploadTask task : persistedTasks) {
            localFiles.add(localFilesByImageId.get(task.image().getId()));
            uploadImageIds.add(task.image().getId());
            originalNames.add(task.image().getOriginalName());
        }
        if (!uploadImageIds.isEmpty()) {
            asyncService.uploadAndSendMsgAsync(uploadImageIds, localFiles, originalNames, imageDTO);
        }
        if (imageIds.isEmpty()) {
            throw new ParamIllegalException("所有文件均处理失败");
        }
        return imageIds;
    }

    private record UploadTask(ImagePO image, MultipartFile file) {
    }
}
