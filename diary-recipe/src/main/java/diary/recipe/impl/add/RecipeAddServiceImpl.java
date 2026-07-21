package diary.recipe.impl.add;

import diary.common.convert.recipe.AoConvertToPo;
import diary.common.convert.recipe.DtoConvertToPo;
import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.entity.recipe.po.RecipeIngredientPO;
import diary.common.entity.recipe.po.RecipePO;
import diary.common.entity.recipe.po.RecipeStepPO;
import diary.common.exception.ParamIllegalException;
import diary.common.exception.SameDataException;
import diary.common.result.ApiResponse;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addRecipe(RecipeReqDto recipeReqDto) {
        validateRecipe(recipeReqDto);

        if (recipeMapper.selectByAuthorTitle(recipeReqDto.getAuthorId(), recipeReqDto.getTitle(), recipeReqDto.getMealType()) != null) {
            throw new SameDataException("作者在该餐别下已存在同名食谱");
        }

        validateIngredients(recipeReqDto);
        validateSteps(recipeReqDto);

        recipeReqDto.setId(MyUtils.getPrimaryKey());
        RecipePO recipePO = DtoConvertToPo.recipeReqDtoConvertToPO(recipeReqDto);
        List<RecipeIngredientPO> ingredientPOs = recipeReqDto.getIngredients().stream()
                .map(ao -> AoConvertToPo.convertToPO(ao, recipePO.getId(), recipePO.getUserId(), MyUtils.getPrimaryKey()))
                .toList();
        List<RecipeStepPO> stepPOs = recipeReqDto.getSteps().stream()
                .map(ao -> AoConvertToPo.convertToPO(ao, recipePO.getId(), recipePO.getUserId(), MyUtils.getPrimaryKey()))
                .toList();

        recipeMapper.insert(recipePO);
        recipeIngredientMapper.batchInsert(ingredientPOs);
        recipeStepMapper.batchInsert(stepPOs);

        return ApiResponse.success("食谱添加成功");
    }

    private void validateRecipe(RecipeReqDto recipeReqDto) {
        if (recipeReqDto == null) {
            throw new ParamIllegalException("入参为空");
        }
        recipeReqDto.setAuthorId(10000L);
        if (recipeReqDto.getAuthorId() == null || recipeReqDto.getTitle() == null
                || recipeReqDto.getTitle().isBlank() || recipeReqDto.getImageId() == null
                || recipeReqDto.getDescription() == null || recipeReqDto.getDescription().isBlank()
                || recipeReqDto.getCategory() == null || recipeReqDto.getMealType() == null
                || recipeReqDto.getDifficulty() == null || recipeReqDto.getCookingTime() == null
                || recipeReqDto.getIngredients() == null || recipeReqDto.getIngredients().isEmpty()
                || recipeReqDto.getSteps() == null || recipeReqDto.getSteps().isEmpty()) {
            throw new ParamIllegalException("食谱存在必填参数为空");
        }
    }

    private void validateIngredients(RecipeReqDto recipeReqDto) {
        recipeReqDto.getIngredients().stream()
                .filter(ingredient -> ingredient.getName() == null || ingredient.getName().isBlank()
                        || ingredient.getQuantity() == null || ingredient.getQuantity().isBlank()
                        || ingredient.getIsMain() == null)
                .findAny()
                .ifPresent(ingredient -> {
                    throw new ParamIllegalException("食材存在必填参数为空");
                });
    }

    private void validateSteps(RecipeReqDto recipeReqDto) {
        recipeReqDto.getSteps().stream()
                .filter(step -> step.getDescription() == null || step.getDescription().isBlank()
                        || step.getStepNumber() == null)
                .findAny()
                .ifPresent(step -> {
                    throw new ParamIllegalException("步骤存在必填参数为空");
                });
    }
}
