package diary.file.impl.deleteserviceImpl;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import diary.common.entity.image.po.ImagePO;
import diary.common.exception.NullResultException;
import diary.common.exception.ParamIllegalException;
import diary.file.mapper.ImageMapper;
import diary.file.service.deleteservice.DeleteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DeleteServiceImpl implements DeleteService {
    @Resource
    private ImageMapper imageMapper;

    @Resource
    private OSS ossClient;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Override
    public String deleteImage(Long id) {
        if (id == null) throw new ParamIllegalException("id不能为空");
        // 查看数据库中是否有当前图片
        ImagePO imagePO = imageMapper.selectImageById(id);
        if (null == imagePO) throw new NullResultException("图片不存在");
        // 删除图片：先删OSS，再删数据库
        String objectKey = imagePO.getObjectKey();
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                ossClient.deleteObject(bucketName, objectKey);
                log.info("OSS删除成功，bucket: {}, key: {}", bucketName, objectKey);
                Integer i = imageMapper.deleteImageById(id);
                if (i > 0) log.info("数据库删除成功，id: {}", id);
                return "删除成功";
            } catch (OSSException e) {
                // 文件不存在 = 成功
                if ("NoSuchKey".equals(e.getErrorCode())) {
                    log.warn("文件已不存在，bucket: {}, key: {}", bucketName, objectKey);
                    Integer i = imageMapper.deleteImageById(id);
                    if (i > 0) log.info("数据库删除成功，id: {}", id);
                    return "删除成功";
                }
                // 不可重试错误直接抛出（保留原始异常）
                if ("AccessDenied".equals(e.getErrorCode()) || "NoSuchBucket".equals(e.getErrorCode())) {
                    throw new RuntimeException("OSS删除失败（不可重试）", e);
                }
                // 最后一次重试失败则抛出
                if (attempt == 3) {
                    throw new RuntimeException(String.format("OSS删除失败，已重试%d次", 3), e);
                }
                log.warn("OSS删除失败，第{}次重试", attempt);
            } catch (ClientException e) {
                if (attempt == 3) {
                    throw new RuntimeException(String.format("OSS删除失败（网络异常），已重试%d次", 3), e);
                }
                log.warn("OSS删除网络异常，第{}次重试", attempt);
            }

            // 重试前等待（指数退避）
            try {
                TimeUnit.SECONDS.sleep((1L << (attempt - 1))); // 1, 2, 4
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("重试被中断", ie);
            }
        }
        return "删除成功";
    }
}
