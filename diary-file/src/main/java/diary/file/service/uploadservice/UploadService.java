package diary.file.service.uploadservice;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface UploadService {
    /**
     * 批量添加文件信息到数据库
     * @param files 文件列表
     * @return 数据库插入结果
     */
    Map<String, Object> addPhotosToDb(List<MultipartFile> files);

    Map<String, Object> addVideoToDb(MultipartFile file);
}
