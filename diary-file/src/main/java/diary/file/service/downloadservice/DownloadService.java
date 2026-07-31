package diary.file.service.downloadservice;

import diary.common.entity.ai.ao.ImageIdUrl;

import java.util.List;
import java.util.Map;

public interface DownloadService {
    /**
     * 批量下载图片
     * @param imageIdUrls OSS图片URL列表和id列表
     * @return 下载结果
     */
    Map<String, Object> batchDownloadPhotos(Map<Long, String> imageIdUrls);
}
