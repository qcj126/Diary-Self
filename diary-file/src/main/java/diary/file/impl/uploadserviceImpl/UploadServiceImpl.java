package diary.file.impl.uploadserviceImpl;

import diary.common.consts.PhotoStatusConst;
import diary.common.consts.PhotoTypeConst;
import diary.common.entity.image.dto.ImageDTO;
import diary.common.entity.image.po.ImagePO;
import diary.common.enums.typeenum.TypeEnum;
import diary.common.exception.ParamIllegalException;
import diary.file.mapper.ImageMapper;
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
    private ImageMapper imagMapper;

    @Override
    public List<Long> addImagesToDb(List<MultipartFile> files, ImageDTO imageDTO) {
        if (files == null || files.isEmpty() || imageDTO == null || imageDTO.getUserId() == null || imageDTO.getCode() == null) {
            throw new ParamIllegalException("文件列表或用户ID或图片类别为空");
        }

        List<ImagePO> imageList = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        // 第一步：验证所有文件并构建Photo对象列表
        for (MultipartFile file : files) {
            try {
                if (isFileEmpty(file)) {
                    failedFiles.add(file.getOriginalFilename() + ": 文件为空");
                    continue;
                }

                // 验证是否为图片类型
                String imageFormat = file.getContentType();
                if (isEmpty(imageFormat) || !imageFormat.startsWith("image")) {
                    try {
                        if (ImageIO.read(file.getInputStream()) == null) {
                            failedFiles.add(file.getOriginalFilename() + ": 文件不是图片类型");
                        }
                    } catch (Exception e) {
                        failedFiles.add(file.getOriginalFilename() + ": 文件读取失败");
                    }
                    continue;
                }

                Integer type = TypeEnum.getCode(imageDTO.getCode());
                String originalFilename = file.getOriginalFilename();

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
                image.setType(TypeEnum.getCode(imageDTO.getCode()));
                image.setStatus(PhotoStatusConst.PHOTO_STATUS_PROCESSING);
                image.setUrl("uploading...");
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
        return imageIds;
    }
}