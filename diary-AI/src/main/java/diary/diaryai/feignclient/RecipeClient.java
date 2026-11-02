package diary.diaryai.feignclient;

import diary.common.result.ApiResponse;
import diary.diaryai.feignclient.config.AiFeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 读取食谱标题；超时和非 2xx 响应统一由 AiFeignClientConfig 管理。
 */
@FeignClient(name = "diary-recipe", path = "/recipe", configuration = AiFeignClientConfig.class)
public interface RecipeClient {
    @GetMapping("/query/{recipeId}")
    ApiResponse<String> getRecipeTitle(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("recipeId") Long recipeId
    );
}
