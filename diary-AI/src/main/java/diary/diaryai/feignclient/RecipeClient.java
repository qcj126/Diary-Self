package diary.diaryai.feignclient;

import diary.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "diary-recipe", path = "/recipe")
public interface RecipeClient {
    @GetMapping("/query/{recipeId}")
    ApiResponse<String> getRecipeTitle(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @PathVariable("recipeId") Long recipeId
    );
}
