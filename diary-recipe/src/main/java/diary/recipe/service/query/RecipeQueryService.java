package diary.recipe.service.query;

import diary.recipe.entity.dto.req.RecipePageReqDto;
import diary.recipe.entity.vo.PageRecipeVO;
import diary.recipe.entity.vo.RecipeCategoryVO;
import diary.recipe.entity.vo.RecipeVO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface RecipeQueryService {
    ApiResponse<PageRecipeVO<RecipeVO>> pageQueryRecipe(RecipePageReqDto pageReqDto);

    ApiResponse<List<RecipeCategoryVO>> queryRecipeCategory();
}
