package diary.recipe.impl.delete;

import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.entity.recipe.po.RecipePO;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
import diary.recipe.mapper.RecipeIngredientMapper;
import diary.recipe.mapper.RecipeMapper;
import diary.recipe.mapper.RecipeStepMapper;
import diary.recipe.service.delete.RecipeDeleteService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeDeleteServiceImpl implements RecipeDeleteService {
    @Resource
    private RecipeMapper recipeMapper;

    @Resource
    private RecipeStepMapper recipeStepMapper;

    @Resource
    private RecipeIngredientMapper recipeIngredientMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> deleteRecipe(RecipeReqDto recipeReqDto) {
        if (recipeReqDto == null || recipeReqDto.getAuthorId() == null) {
            throw new ParamIllegalException("入参或作者ID为空");
        }

        RecipePO recipePO = getRecipe(recipeReqDto);
        if (recipePO == null) {
            throw new ParamIllegalException("未找到食谱，无法执行删除操作");
        }

        recipeMapper.updateStatus(recipePO.getId(), 1, recipeReqDto.getAuthorId());
        recipeIngredientMapper.deleteByRecipeId(recipePO.getId());
        recipeStepMapper.deleteByRecipeId(recipePO.getId());

        return ApiResponse.success("删除成功");
    }

    private RecipePO getRecipe(RecipeReqDto recipeReqDto) {
        if (recipeReqDto.getId() != null) {
            RecipePO recipePO = recipeMapper.selectById(recipeReqDto.getId());
            if (recipePO != null && recipeReqDto.getAuthorId().equals(recipePO.getUserId())) {
                return recipePO;
            }
            return null;
        }
        if (recipeReqDto.getTitle() == null || recipeReqDto.getTitle().isBlank() || recipeReqDto.getMealType() == null) {
            throw new ParamIllegalException("食谱ID为空时，作者ID、标题、餐别不能为空");
        }
        return recipeMapper.selectByAuthorTitle(recipeReqDto.getAuthorId(), recipeReqDto.getTitle(), recipeReqDto.getMealType());
    }
}
