package diary.file.impl.downloadserviceImpl;

import diary.file.service.asyncservice.AsyncService;
import diary.file.service.downloadservice.DownloadService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DownloadServiceImpl implements DownloadService {
    @Value("${download.path:C:\\Users\\admin\\Pictures\\Saved Pictures}")
    private String defaultDownloadPath;

    @Resource
    private AsyncService asyncService;

    @Override
    public Map<String, Object> batchDownloadPhotos(Map<Long, String>  imageIdUrls) {
        MyUtils.check().mapNotContainsEmpty(imageIdUrls, "图片id或url").notNull(imageIdUrls, "imageIdUrls");

        // 确保下载目录存在
        Path downloadDir = Paths.get(defaultDownloadPath);
        try {
            if (!Files.exists(downloadDir)) {
                Files.createDirectories(downloadDir);
                log.info("创建下载目录: {}", defaultDownloadPath);
            }
        } catch (IOException e) {
            log.error("创建下载目录失败: {}", defaultDownloadPath, e);
            return new HashMap<>();
        }

        // 用于存储所有下载任务的结果
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();

        // 并发计数器
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 提交所有下载任务
        for (Long imageId : imageIdUrls.keySet()) {
            String url = imageIdUrls.get(imageId);
            CompletableFuture<Map<String, Object>> future = asyncService.downloadImageAsync(imageId, url, defaultDownloadPath)
                    .thenApply(result -> {
                        if ("success".equals(result.get("status"))) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                        return result;
                    });
            futures.add(future);
        }
        // 将任务加入allFutures中，阻塞等待所有任务完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])); // 等待所有任务完成，传入一个初始长度为 0 的数组，JVM 会优化并直接分配一个大小刚好的新数组

        try {
            // 阻塞等待所有任务完成
            allFutures.join();

            List<String> failedFiles = new ArrayList<>();
            Map<Long, String> downloadfilePathIdMap = new HashMap<>();
            for (CompletableFuture<Map<String, Object>> future : futures) {
                Map<String, Object> result = future.get();

                String ossUrl = String.valueOf(result.get("ossUrl"));
                if ("success".equals(result.get("status"))) {
                    downloadfilePathIdMap.put(Long.parseLong(String.valueOf(result.get("imageId"))), String.valueOf(result.get("filePath")));
                } else {
                    failedFiles.add(ossUrl + ": " + result.get("message"));
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", String.format("下载完成，成功: %d, 失败: %d", successCount.get(), failCount.get()));
            response.put("total", imageIdUrls.size());
            response.put("successCount", successCount.get());
            response.put("failCount", failCount.get());
            response.put("successFiles", downloadfilePathIdMap);
            if (!failedFiles.isEmpty()) {
                response.put("failedFiles", failedFiles);
            }

            log.info("批量下载完成，总数: {}, 成功: {}, 失败: {}", imageIdUrls.size(), successCount.get(), failCount.get());

            return response;
        } catch (Exception e) {
            log.error("批量下载异常", e);
            return Map.of("code", 500, "message", "批量下载异常: " + e.getMessage(), "data", "null");
        }
    }
}
