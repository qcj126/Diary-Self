package diary.file.service.asyncservice;

import diary.common.entity.image.dto.ImageDTO;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Image;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface AsyncService {
    /**
     * 异步上传图片到OSS并发送消息
     *
     * @param result 数据库插入结果
     * @param files  文件列表
     */
    void uploadAndSendMsgAsync(List<Long> result, List<MultipartFile> files, ImageDTO imageDTO);

    /**
     * 异步下载单个图片
     * @param ossUrl OSS图片URL
     * @param savePath 保存路径
     * @return 下载结果
     */
    CompletableFuture<Map<String, Object>> downloadImageAsync(String ossUrl, String savePath);

    /**
     * 异步上传单个文件到OSS并发送消息
     * @param result 数据库插入结果
     * @param file 文件
     */
    void uploadAndSendMsgAsync(Map<String, Object> result, MultipartFile file);
}
