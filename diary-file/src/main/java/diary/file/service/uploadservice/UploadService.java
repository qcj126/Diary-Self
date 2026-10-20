package diary.file.service.uploadservice;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UploadService {
    /**
     * 批量添加文件信息到数据库
     *
     * @param files  文件列表
     * @param code   分类代码
     * @return 数据库插入结果
     */
    List<Long> uploadImagesAndInsert(List<MultipartFile> files, Integer code);
}
