package diary.diaryai.feignclient;

import diary.common.result.ApiResponse;
import diary.diaryai.feignclient.config.AiFeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 读取饮食记录的展示名称；超时和非 2xx 响应统一由 AiFeignClientConfig 管理。
 */
@FeignClient(name = "diary-diet", path = "/diet", configuration = AiFeignClientConfig.class)
public interface DietClient {
    @GetMapping("/query/food-name/{dietId}")
    ApiResponse<String> getDietFoodName(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("dietId") Long dietId
    );
}
