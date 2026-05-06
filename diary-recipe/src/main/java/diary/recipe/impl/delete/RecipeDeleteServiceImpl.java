package diary.recipe.impl.delete;

import diary.common.result.ApiResponse;
import diary.common.entity.recipe.dto.RecipeReqDto;
import org.springframework.stereotype.Service;
import diary.recipe.service.delete.RecipeDeleteService;

import java.util.Map;

@Service
public class RecipeDeleteServiceImpl implements RecipeDeleteService {
    @Override
    public ApiResponse<Map<String, Object>> deleteRecipe(RecipeReqDto recipeReqDto) {
        return null;
    }
}
