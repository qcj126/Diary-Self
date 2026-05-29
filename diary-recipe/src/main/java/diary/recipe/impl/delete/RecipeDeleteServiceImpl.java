package diary.recipe.impl.delete;

import diary.common.entity.recipe.po.RecipePO;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
import diary.common.entity.recipe.dto.req.RecipeReqDto;

import diary.recipe.mapper.RecipeIngredientMapper;
import diary.recipe.mapper.RecipeMapper;
import diary.recipe.mapper.RecipeStepMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import diary.recipe.service.delete.RecipeDeleteService;

@Service
public class RecipeDeleteServiceImpl implements RecipeDeleteService {
    @Resource
    private RecipeMapper recipeMapper;

    @Resource
    private RecipeStepMapper recipeStepMapper;

    @Resource
    private RecipeIngredientMapper recipeIngredientMapper;

    @Override
    public ApiResponse<String> deleteRecipe(RecipeReqDto recipeReqDto) {
        // 根据authorId和title删除食谱和食材和步骤
        if (recipeReqDto.getAuthorId() == null || recipeReqDto.getTitle() == null || recipeReqDto.getMealType() == null) {
            throw new ParamIllegalException("作者ID或标题或餐类为空");
        }

        // 查询有无此食谱
        RecipePO recipePO = recipeMapper.selectByAuthorTitle(recipeReqDto.getAuthorId(), recipeReqDto.getTitle(), recipeReqDto.getMealType());
        if (recipePO == null) {
            throw new ParamIllegalException("此分类下无此食谱，无法执行删除操作");
        }
        // 根据食谱id删除食材和步骤数据
        recipeIngredientMapper.deleteByRecipeId(recipePO.getRecipeId());
        recipeStepMapper.deleteByRecipeId(recipePO.getRecipeId());

        return ApiResponse.success("删除成功");
    }
}
