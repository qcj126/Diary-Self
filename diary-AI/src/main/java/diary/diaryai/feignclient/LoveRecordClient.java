package diary.diaryai.feignclient;

import diary.common.entity.feign.LoveRecordAiSnapshotDTO;
import diary.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "diary-love", path = "/love")
public interface LoveRecordClient {
    @GetMapping("/query/love-record/{loveRecordId}")
    ApiResponse<LoveRecordAiSnapshotDTO> getLoveRecordAiSnapshot(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("loveRecordId") Long loveRecordId
    );
}
