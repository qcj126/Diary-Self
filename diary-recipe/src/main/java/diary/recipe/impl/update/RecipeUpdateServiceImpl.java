package diary.recipe.impl.update;


import diary.common.entity.recipe.ao.RecipeIngredientAO;
import diary.common.entity.recipe.ao.RecipeStepAO;
import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.entity.recipe.po.RecipePO;
import diary.common.exception.NullResultException;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
import diary.recipe.mapper.RecipeIngredientMapper;
import diary.recipe.mapper.RecipeMapper;
import diary.recipe.mapper.RecipeStepMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import diary.recipe.service.update.RecipeUpdateService;

import java.util.List;

@Service
public class RecipeUpdateServiceImpl implements RecipeUpdateService {
    @Resource
    private RecipeMapper recipeMapper;

    @Resource
    private RecipeIngredientMapper recipeIngredientMapper;

    @Resource
    private RecipeStepMapper recipeStepMapper;

    @Override
    public ApiResponse<String> updateRecipe(RecipeReqDto recipeReqDto) {
        if (recipeReqDto.getAuthorId() == null || recipeReqDto.getTitle() == null || recipeReqDto.getMealType() == null) {
            throw new ParamIllegalException("作者ID或标题或餐类为空");
        }
        RecipePO recipePO = recipeMapper.selectByAuthorTitle(recipeReqDto.getAuthorId(), recipeReqDto.getTitle(), recipeReqDto.getMealType());
        if (recipePO == null) {
            throw new NullResultException("此分类下无此食谱，无法执行更新操作");
        }
        List<RecipeIngredientAO> ingredients = recipeReqDto.getIngredients();
        List<RecipeStepAO> steps = recipeReqDto.getSteps();
        if (!ingredients.isEmpty() && ingredients != null) {
            recipeIngredientMapper.batchUpdate(ingredients, recipePO.getId());
        }
        if (!steps.isEmpty() && steps != null) {
            recipeStepMapper.batchUpdate(steps, recipePO.getId());
        }
        recipeMapper.updateById(recipeReqDto, recipePO.getId());
        return ApiResponse.success("已更新");
    }
}
