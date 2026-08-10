package diary.recipe.impl.delete;

import diary.common.entity.recipe.dto.req.RecipeCategoryDto;
import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.entity.recipe.po.RecipePO;
import diary.common.result.ApiResponse;
import diary.recipe.mapper.RecipeCategoryMapper;
import diary.recipe.mapper.RecipeIngredientMapper;
import diary.recipe.mapper.RecipeMapper;
import diary.recipe.mapper.RecipeStepMapper;
import diary.recipe.service.delete.RecipeDeleteService;
import diary.utils.commonutil.MyUtils;
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

    @Resource
    private RecipeCategoryMapper recipeCategoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> deleteRecipe(RecipeReqDto recipeReqDto) {
        validateRecipeQueryParam(recipeReqDto);

        RecipePO recipePO = recipeMapper.selectById(recipeReqDto.getRecipeId());
        if (recipePO == null) {
            return ApiResponse.delFail();
        }

        int cnt = recipeMapper.deleteRecipeById(recipePO.getId());
        if (cnt <= 0) {
            return ApiResponse.delFail();
        }
        recipeIngredientMapper.deleteByRecipeId(recipePO.getId());
        recipeStepMapper.deleteByRecipeId(recipePO.getId());

        return ApiResponse.success("删除成功");
    }

    @Override
    public ApiResponse<String> deleteCategory(RecipeCategoryDto recipeCategoryDto) {
        MyUtils.check()
                .notNull(recipeCategoryDto, "食谱分类编号列表")
                .notNull(recipeCategoryDto.getCategoryIds(), "食谱分类编号列表")
                .listNotEmpty(recipeCategoryDto.getCategoryIds(), "食谱分类编号列表")
                .listNotContainsEmpty(recipeCategoryDto.getCategoryIds(), "食谱分类编号列表");
        Integer i = recipeCategoryMapper.deleteByIds(recipeCategoryDto.getCategoryIds());
        if (i != null && i > 0) {
            return ApiResponse.success("删除成功");
        }
        return ApiResponse.delFail();
    }

    private void validateRecipeQueryParam(RecipeReqDto recipeReqDto) {
        MyUtils.check()
                .notNull(recipeReqDto, "食谱")
                .notNull(recipeReqDto.getRecipeId(), "食谱id");
    }
}
