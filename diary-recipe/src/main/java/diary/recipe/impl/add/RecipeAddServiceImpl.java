package diary.recipe.impl.add;

import diary.common.convert.recipe.AoConvertToPo;
import diary.common.convert.recipe.DtoConvertToPo;
import diary.common.entity.recipe.dto.req.RecipeCategoryDto;
import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.entity.recipe.po.RecipeCategoryPO;
import diary.common.entity.recipe.po.RecipeIngredientPO;
import diary.common.entity.recipe.po.RecipePO;
import diary.common.entity.recipe.po.RecipeStepPO;
import diary.common.exception.ParamIllegalException;
import diary.common.exception.SameDataException;
import diary.common.result.ApiResponse;
import diary.recipe.mapper.RecipeCategoryMapper;
import diary.recipe.mapper.RecipeIngredientMapper;
import diary.recipe.mapper.RecipeMapper;
import diary.recipe.mapper.RecipeStepMapper;
import diary.recipe.service.add.RecipeAddService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecipeAddServiceImpl implements RecipeAddService {
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
    public ApiResponse<String> addRecipe(RecipeReqDto recipeReqDto) {
        validateRecipe(recipeReqDto);

        if (recipeMapper.selectByAuthorTitle(recipeReqDto.getTitle(), recipeReqDto.getMealType()) != null) {
            throw new SameDataException("该餐别下已存在同名食谱");
        }

        validateIngredients(recipeReqDto);
        validateSteps(recipeReqDto);

        recipeReqDto.setRecipeId(MyUtils.getPrimaryKey());
        RecipePO recipePO = DtoConvertToPo.recipeReqDtoConvertToPO(recipeReqDto);
        List<RecipeIngredientPO> ingredientPOs = recipeReqDto.getIngredients().stream()
                .map(ao -> AoConvertToPo.convertToPO(ao, recipePO.getId(), recipePO.getUserId(), MyUtils.getPrimaryKey()))
                .toList();
        List<RecipeStepPO> stepPOs = recipeReqDto.getSteps().stream()
                .map(ao -> AoConvertToPo.convertToPO(ao, recipePO.getId(), recipePO.getUserId(), MyUtils.getPrimaryKey()))
                .toList();

        Integer cnt = recipeMapper.insert(recipePO);
        if (cnt == null || cnt <= 0) {
            return ApiResponse.addFail();
        }
        recipeIngredientMapper.batchInsert(ingredientPOs);
        recipeStepMapper.batchInsert(stepPOs);

        return ApiResponse.success("添加成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addCategory(RecipeCategoryDto recipeCategoryDto) {
        MyUtils.check()
                .notNull(recipeCategoryDto, "食谱分类")
                .notEmpty(recipeCategoryDto.getCategoryName(), "食谱分类名称")
                .notNull(recipeCategoryDto.getIconId(), "食谱分类图标id");

        Integer maxCategoryNum = recipeCategoryMapper.selectMaxCategoryNum();
        Integer categoryNum = maxCategoryNum == null ? 1000 : maxCategoryNum + 100;
        recipeCategoryDto.setCategoryId(MyUtils.getPrimaryKey());
        recipeCategoryDto.setUserId(10000L);
        recipeCategoryDto.setCategoryNum(categoryNum);
        RecipeCategoryPO recipeCategoryPO = DtoConvertToPo.recipeCategoryDtoConvertToPO(recipeCategoryDto);
        Integer cnt = recipeCategoryMapper.insert(recipeCategoryPO);
        if (cnt != null && cnt > 0) {
            return ApiResponse.success("添加成功");
        }
        return ApiResponse.addFail();
    }

    private void validateRecipe(RecipeReqDto recipeReqDto) {
        MyUtils.check().notNull(recipeReqDto, "食谱");
        recipeReqDto.setAuthorId(10000L);
        MyUtils.check()
                .notNull(recipeReqDto.getAuthorId(), "食谱作者编号")
                .notEmpty(recipeReqDto.getTitle(), "食谱标题")
                .notNull(recipeReqDto.getImageId(), "食谱封面")
                .notEmpty(recipeReqDto.getDescription(), "食谱简介")
                .notNull(recipeReqDto.getCategoryNum(), "食谱分类编号")
                .notNull(recipeReqDto.getMealType(), "餐别")
                .notNull(recipeReqDto.getDifficulty(), "难度")
                .notNull(recipeReqDto.getCookingTime(), "烹饪时长")
                .notNull(recipeReqDto.getIngredients(), "食材")
                .listNotEmpty(recipeReqDto.getIngredients(), "食材")
                .listNotContainsEmpty(recipeReqDto.getIngredients(), "食材")
                .notNull(recipeReqDto.getSteps(), "步骤")
                .listNotEmpty(recipeReqDto.getSteps(), "步骤")
                .listNotContainsEmpty(recipeReqDto.getSteps(), "步骤");
    }

    private void validateIngredients(RecipeReqDto recipeReqDto) {
        recipeReqDto.getIngredients().stream()
                .filter(ingredient -> MyUtils.isEmpty(ingredient.getName())
                        || MyUtils.isEmpty(ingredient.getQuantity())
                        || ingredient.getIsMain() == null)
                .findAny()
                .ifPresent(ingredient -> {
                    throw new ParamIllegalException("食材存在必填参数为空");
                });
    }

    private void validateSteps(RecipeReqDto recipeReqDto) {
        recipeReqDto.getSteps().stream()
                .filter(step -> MyUtils.isEmpty(step.getDescription()) || step.getStepNumber() == null)
                .findAny()
                .ifPresent(step -> {
                    throw new ParamIllegalException("步骤存在必填参数为空");
                });
    }
}
