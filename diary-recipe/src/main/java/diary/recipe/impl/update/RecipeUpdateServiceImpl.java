package diary.recipe.impl.update;

import diary.common.convert.recipe.AoConvertToPo;
import diary.common.convert.recipe.DtoConvertToPo;
import diary.common.entity.recipe.dto.req.RecipeCategoryDto;
import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.entity.recipe.po.RecipeCategoryPO;
import diary.common.entity.recipe.po.RecipeIngredientPO;
import diary.common.entity.recipe.po.RecipePO;
import diary.common.entity.recipe.po.RecipeStepPO;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
import diary.recipe.mapper.RecipeCategoryMapper;
import diary.recipe.mapper.RecipeIngredientMapper;
import diary.recipe.mapper.RecipeMapper;
import diary.recipe.mapper.RecipeStepMapper;
import diary.recipe.service.update.RecipeUpdateService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecipeUpdateServiceImpl implements RecipeUpdateService {
    @Resource
    private RecipeMapper recipeMapper;

    @Resource
    private RecipeIngredientMapper recipeIngredientMapper;

    @Resource
    private RecipeStepMapper recipeStepMapper;

    @Resource
    private RecipeCategoryMapper recipeCategoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateRecipe(RecipeReqDto recipeReqDto) {
        validateRecipeQueryParam(recipeReqDto);

        RecipePO recipePO = recipeMapper.selectById(recipeReqDto.getRecipeId());
        if (recipePO == null) {
            return ApiResponse.updateFail();
        }

        Integer cnt = recipeMapper.updateById(recipeReqDto, recipePO.getId());
        if (cnt == null || cnt <= 0) {
            return ApiResponse.updateFail();
        }
        replaceIngredients(recipeReqDto, recipePO);
        replaceSteps(recipeReqDto, recipePO);
        return ApiResponse.success("修改成功");
    }

    @Override
    public ApiResponse<String> updateCategory(RecipeCategoryDto recipeCategoryDto) {
        MyUtils.check()
                .notNull(recipeCategoryDto, "食谱分类")
                .notNull(recipeCategoryDto.getCategoryId(), "食谱分类编号")
                .notEmpty(recipeCategoryDto.getCategoryName(), "食谱分类名称")
                .notNull(recipeCategoryDto.getIconId(), "食谱分类图标id");
        RecipeCategoryPO recipeCategoryPO = DtoConvertToPo.recipeCategoryDtoConvertToPO(recipeCategoryDto);
        recipeCategoryPO.setUpdateTime(LocalDateTime.now());
        int cnt = recipeCategoryMapper.updateCategoryById(recipeCategoryPO);
        if (cnt > 0) {
            return ApiResponse.success("修改成功");
        }
        return ApiResponse.updateFail();
    }

    private void validateRecipeQueryParam(RecipeReqDto recipeReqDto) {
        MyUtils.check()
                .notNull(recipeReqDto, "食谱")
                .notEmpty(recipeReqDto.getTitle(), "食谱标题")
                .notNull(recipeReqDto.getMealType(), "餐别")
                .notNull(recipeReqDto.getCategoryNum(), "分类编号");
    }

    private void replaceIngredients(RecipeReqDto recipeReqDto, RecipePO recipePO) {
        if (recipeReqDto.getIngredients() == null) {
            return;
        }
        recipeIngredientMapper.deleteByRecipeId(recipePO.getId());
        if (recipeReqDto.getIngredients().isEmpty()) {
            return;
        }
        recipeReqDto.getIngredients().stream()
                .filter(ingredient -> ingredient == null
                        || MyUtils.isEmpty(ingredient.getName())
                        || MyUtils.isEmpty(ingredient.getQuantity())
                        || ingredient.getIsMain() == null)
                .findAny()
                .ifPresent(ingredient -> {
                    throw new ParamIllegalException("食材存在必填参数为空");
                });
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
        recipeReqDto.getSteps().stream()
                .filter(step -> step == null || MyUtils.isEmpty(step.getDescription()) || step.getStepNumber() == null)
                .findAny()
                .ifPresent(step -> {
                    throw new ParamIllegalException("步骤存在必填参数为空");
                });
        List<RecipeStepPO> stepPOs = recipeReqDto.getSteps().stream()
                .map(ao -> AoConvertToPo.convertToPO(ao, recipePO.getId(), recipePO.getUserId(), MyUtils.getPrimaryKey()))
                .toList();
        recipeStepMapper.batchInsert(stepPOs);
    }
}
