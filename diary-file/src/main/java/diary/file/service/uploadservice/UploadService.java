package diary.file.service.uploadservice;

import diary.common.entity.image.dto.ImageDTO;
import diary.common.entity.image.vo.ImageVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface UploadService {
    /**
     * 批量添加文件信息到数据库
     *
     * @param files  文件列表
     * @param imageDTO
     * @return 数据库插入结果
     */
    List<Long> addImagesToDb(List<MultipartFile> files, ImageDTO imageDTO);
}
