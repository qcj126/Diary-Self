package diary.recipe.impl.update;

import diary.common.convert.recipe.AoConvertToPo;
import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.entity.recipe.po.RecipeIngredientPO;
import diary.common.entity.recipe.po.RecipePO;
import diary.common.entity.recipe.po.RecipeStepPO;
import diary.common.exception.NullResultException;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
import diary.recipe.mapper.RecipeIngredientMapper;
import diary.recipe.mapper.RecipeMapper;
import diary.recipe.mapper.RecipeStepMapper;
import diary.recipe.service.update.RecipeUpdateService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateRecipe(RecipeReqDto recipeReqDto) {
        if (recipeReqDto == null || recipeReqDto.getAuthorId() == null) {
            throw new ParamIllegalException("入参或作者ID为空");
        }

        RecipePO recipePO = getRecipe(recipeReqDto);
        if (recipePO == null) {
            throw new NullResultException("未找到食谱，无法执行更新操作");
        }

        recipeMapper.updateById(recipeReqDto, recipePO.getId());
        replaceIngredients(recipeReqDto, recipePO);
        replaceSteps(recipeReqDto, recipePO);
        return ApiResponse.success("已更新");
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

    private void replaceIngredients(RecipeReqDto recipeReqDto, RecipePO recipePO) {
        if (recipeReqDto.getIngredients() == null) {
            return;
        }
        recipeIngredientMapper.deleteByRecipeId(recipePO.getId());
        if (recipeReqDto.getIngredients().isEmpty()) {
            return;
        }
        List<RecipeIngredientPO> ingredientPOs = recipeReqDto.getIngredients().stream()
                .map(ao -> AoConvertToPo.convertToPO(ao, recipePO.getId(), recipePO.getUserId(), MyUtils.getPrimaryKey()))
                .toList();
        recipeIngredientMapper.batchInsert(ingredientPOs);
    }

    private void replaceSteps(RecipeReqDto recipeReqDto, RecipePO recipePO) {
        if (recipeReqDto.getSteps() == null) {
            return;
        }
        recipeStepMapper.deleteByRecipeId(recipePO.getId());
        if (recipeReqDto.getSteps().isEmpty()) {
            return;
        }
        List<RecipeStepPO> stepPOs = recipeReqDto.getSteps().stream()
                .map(ao -> AoConvertToPo.convertToPO(ao, recipePO.getId(), recipePO.getUserId(), MyUtils.getPrimaryKey()))
                .toList();
        recipeStepMapper.batchInsert(stepPOs);
    }
}
