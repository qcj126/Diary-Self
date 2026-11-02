package diary.diaryai.feignclient;

import diary.common.entity.feign.LoveRecordAiSnapshotDTO;
import diary.common.result.ApiResponse;
import diary.diaryai.feignclient.config.AiFeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 读取恋爱记录的 AI 输入快照；超时和非 2xx 响应统一由 AiFeignClientConfig 管理。
 */
@FeignClient(name = "diary-love", path = "/love", configuration = AiFeignClientConfig.class)
public interface LoveRecordClient {
    @GetMapping("/query/love-record/{loveRecordId}")
    ApiResponse<LoveRecordAiSnapshotDTO> getLoveRecordAiSnapshot(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("loveRecordId") Long loveRecordId
    );
}
