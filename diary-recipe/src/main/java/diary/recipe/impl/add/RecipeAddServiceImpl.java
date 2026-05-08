package diary.recipe.impl.add;

import diary.common.convert.AoConvertToPo;
import diary.common.convert.DtoConvertToPo;
import diary.common.exception.ParamIllegalException;
import diary.common.exception.SameDataException;
import diary.common.result.ApiResponse;
import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.entity.recipe.po.RecipeIngredientPO;
import diary.common.entity.recipe.po.RecipePO;
import diary.common.entity.recipe.po.RecipeStepPO;
import diary.dao.mapper.recipe.RecipeIngredientMapper;
import diary.dao.mapper.recipe.RecipeMapper;
import diary.dao.mapper.recipe.RecipeStepMapper;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import diary.recipe.service.add.RecipeAddService;

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
    public ApiResponse<String> addRecipe(RecipeReqDto recipeReqDto) {
        if (recipeReqDto == null) return ApiResponse.fail(400, "入参为空");
        if (recipeReqDto.getTitle() == null ||
            recipeReqDto.getCategory() == null || recipeReqDto.getMealType() == null ||
            recipeReqDto.getIngredients() == null || recipeReqDto.getSteps() == null) {
            throw new ParamIllegalException("食谱存在必填参数为空");
        }
        // 同一作者、同一食谱分类下，不能存在相同食谱名称
        if (recipeMapper.selectByAuthorTitle(recipeReqDto.getAuthorId(), recipeReqDto.getTitle(), recipeReqDto.getMealType()) > 0) {
            throw new SameDataException("作者在该分类下已存在同名食谱");
        }

        recipeReqDto.getIngredients().stream().filter(ingredient -> ingredient.getName() == null || ingredient.getQuantity() == null)
            .findAny().ifPresent(ingredient -> {throw new ParamIllegalException("食材存在必填参数为空");});

        recipeReqDto.getSteps().stream().filter(step -> step.getDescription() == null || step.getStepNumber() == null)
            .findAny().ifPresent(step -> {throw new ParamIllegalException("步骤存在必填参数为空");});

        // 类型转换
        RecipePO recipePO = DtoConvertToPo.convertToPO(recipeReqDto, MyUtils.getPrimaryKey());
        List<RecipeIngredientPO> recipeIngredientPOs = recipeReqDto.getIngredients().stream().map(ao -> AoConvertToPo.convertToPO(ao, recipePO.getRecipeId(), MyUtils.getPrimaryKey())).toList();
        List<RecipeStepPO> recipeStepPOs = recipeReqDto.getSteps().stream().map(ao -> AoConvertToPo.convertToPO(ao, recipePO.getRecipeId(), MyUtils.getPrimaryKey())).toList();

        recipeMapper.insert(recipePO);
        recipeIngredientMapper.batchInsert(recipeIngredientPOs);
        recipeStepMapper.batchInsert(recipeStepPOs);
        
        return ApiResponse.success("食谱添加成功");
    }
}
