package diary.file.impl.uploadserviceImpl;

import diary.common.consts.PhotoStatusConst;
import diary.common.entity.image.dto.ImageDTO;
import diary.common.entity.image.po.ImagePO;
import diary.common.enums.typeenum.TypeEnum;
import diary.common.exception.ParamIllegalException;
import diary.file.mapper.ImageMapper;
import diary.file.service.asyncservice.AsyncService;
import diary.file.service.uploadservice.UploadService;

import diary.utils.commonutil.MyUtils;
import diary.utils.file.FileUtil;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static diary.utils.commonutil.MyUtils.isEmpty;
import static diary.utils.commonutil.MyUtils.isFileEmpty;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {
    private final ImageMapper imagMapper;

    private final FileUtil fileUtil;

    private final AsyncService asyncService;

    @Override
    public List<Long> uploadImagesAndInsert(List<MultipartFile> files, Integer code) {
        // 拦截参数为空的数据
        MyUtils.check().notNull(files, "文件列表").listNotContainsEmpty(files, "文件列表").notNull(code, "图片类别");

        // 过滤非图片文件
        files = files.stream()
                .filter(file -> {
                    MyUtils.check().notEmpty(file.getContentType(), "文件类型");
                    return file.getContentType().equals("image");
                })
                .filter(file -> {
                    try {
                        return ImageIO.read(file.getInputStream()) != null;
                    } catch (Exception e) {
                        return false;
                    }
                }).toList();

        ImageDTO imageDTO = new ImageDTO();
        imageDTO.setUserId(10000L);
        imageDTO.setCode(code);

        List<ImagePO> imageList = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        // 第一步：验证所有文件并构建Photo对象列表
        for (MultipartFile file : files) {
            try {
                Integer type = TypeEnum.getCode(imageDTO.getCode());
                String typeName = TypeEnum.getType(imageDTO.getCode());
                String originalFilename = file.getOriginalFilename();
                String objectKey = fileUtil.getFileName(typeName, originalFilename);

                // 查看同一图片所属类别下是否有相同名称的图片
                Integer isExist = imagMapper.selectImageByTypeAndName(type, originalFilename);
                if (isExist != null && isExist > 0) {
                    failedFiles.add(originalFilename + ": 图片已存在");
                    continue;
                }

                // 构建Photo对象（暂不设置sortOrder）
                ImagePO image = new ImagePO();
                image.setId(MyUtils.getPrimaryKey());
                image.setUserId(imageDTO.getUserId());
                image.setFileSize(file.getSize());
                image.setOriginalName(file.getOriginalFilename());
                image.setMimeType(file.getContentType());
                image.setType(type);
                image.setStatus(PhotoStatusConst.PHOTO_STATUS_PROCESSING);
                image.setObjectKey(objectKey);
                imageList.add(image);
            } catch (Exception e) {
                log.error("处理文件 {} 时发生异常", file.getOriginalFilename(), e);
                failedFiles.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        // 第二步：分批插入数据库，每批最多20条
        List<Long> imageIds = new ArrayList<>();
        int batchSize = 20;
        int totalSize = imageList.size();

        for (int i = 0; i < totalSize; i += batchSize) {
            // 计算当前批次的结束位置
            int end = Math.min(i + batchSize, totalSize);
            List<ImagePO> batchList = imageList.subList(i, end);

            try {
                Integer count = imagMapper.batchAddImageToDb(batchList);
                if (count != null && count > 0) {
                    // 收集成功插入的id
                    for (ImagePO image : batchList) {
                        imageIds.add(image.getId());
                    }
                } else {
                    // 记录失败的文件
                    for (ImagePO image : batchList) {
                        failedFiles.add(image.getOriginalName() + ": 批量插入失败");
                    }
                    log.error("批量插入照片失败，批次范围: {} - {}", i + 1, end);
                }
            } catch (Exception e) {
                log.error("批量插入照片异常，批次范围: {} - {}", i + 1, end, e);
                // 记录失败的文件
                for (ImagePO image : batchList) {
                    failedFiles.add(image.getOriginalName() + ": " + e.getMessage());
                }
            }
        }

        // TODO 后续处理插入失败的情况】
        if (!failedFiles.isEmpty()) {
            log.error("处理文件列表时发生异常，失败文件列表: {}", failedFiles);
        }

        if (imageIds.isEmpty()) {
            throw new ParamIllegalException("所有文件均处理失败");
        }
        // 异步上传图片到OSS成功后，发送消息给mq
        List<File> tempFiles = fileUtil.copyToTempFiles(files);
        List<String> originalNames = files.stream().map(MultipartFile::getOriginalFilename).toList();
        asyncService.uploadAndSendMsgAsync(imageIds, tempFiles, originalNames, imageDTO);
        return imageIds;
    }
}
