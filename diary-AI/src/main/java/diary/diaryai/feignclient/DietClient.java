package diary.diaryai.feignclient;

import diary.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "diary-diet", path = "/diet")
public interface DietClient {
    @GetMapping("/query/food-name/{dietId}")
    ApiResponse<String> getDietFoodName(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("dietId") Long dietId);
}
