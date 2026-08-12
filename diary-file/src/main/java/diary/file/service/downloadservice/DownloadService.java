package diary.file.service.downloadservice;

import java.util.Map;

public interface DownloadService {
    /**
     * 批量下载图片
     * @param imageIdUrls OSS图片URL列表和id列表
     * @return 下载结果
     */
    Map<String, Object> batchDownloadPhotos(Map<Long, String> imageIdUrls);
}
