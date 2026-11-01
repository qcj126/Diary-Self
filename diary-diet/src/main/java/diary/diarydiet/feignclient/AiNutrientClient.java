package diary.diarydiet.feignclient;

import diary.common.entity.ai.dto.AiNutrientBatchQueryDTO;
import diary.common.entity.ai.vo.AiNutrientVO;
import diary.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "diary-ai", path = "/ai")
public interface AiNutrientClient {
    @PostMapping("/nutrient/query/diets/latest")
    ApiResponse<List<AiNutrientVO>> getLatestDietNutrients(
            @RequestHeader("X-Auth-User-Id") Long userId,
            @RequestBody AiNutrientBatchQueryDTO request
    );
}
