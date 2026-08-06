package diary.recipe.impl.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import diary.common.convert.recipe.PoConvertToVo;
import diary.common.entity.recipe.dto.RecipePageReqDto;
import diary.common.entity.recipe.po.RecipeCategoryPO;
import diary.common.entity.recipe.vo.PageRecipeVO;
import diary.common.entity.recipe.po.RecipeIngredientPO;
import diary.common.entity.recipe.po.RecipePO;
import diary.common.entity.recipe.po.RecipeStepPO;
import diary.common.entity.recipe.vo.RecipeCategoryVO;
import diary.common.entity.recipe.vo.RecipeVO;
import diary.common.result.ApiResponse;
import diary.recipe.mapper.RecipeCategoryMapper;
import diary.recipe.mapper.RecipeIngredientMapper;
import diary.recipe.mapper.RecipeMapper;
import diary.recipe.mapper.RecipeStepMapper;
import diary.recipe.service.query.RecipeQueryService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecipeQueryServiceImpl implements RecipeQueryService {
    @Resource
    private RecipeMapper recipeMapper;

    @Resource
    private RecipeIngredientMapper recipeIngredientMapper;

    @Resource
    private RecipeStepMapper recipeStepMapper;

    @Resource
    private RecipeCategoryMapper recipeCategoryMapper;

    @Override
    public PageRecipeVO<RecipeVO> pageQueryRecipe(RecipePageReqDto recipePageReqDto) {
        if (recipePageReqDto == null) {
            recipePageReqDto = new RecipePageReqDto();
        }
        if (recipePageReqDto.getPageNum() == null) {
            recipePageReqDto.setPageNum(1);
        }
        if (recipePageReqDto.getPageSize() == null) {
            recipePageReqDto.setPageSize(20);
        }
        IPage<RecipePO> page = new Page<>(recipePageReqDto.getPageNum(), recipePageReqDto.getPageSize());
        IPage<RecipePO> pageResult = recipeMapper.qryPage(page, recipePageReqDto);
        List<RecipePO> records = pageResult.getRecords();
        MyUtils.check().listNotEmpty(records, "recipe");
        List<Long> recipeIds = records.stream().map(RecipePO::getId).toList();

        Map<Long, List<RecipeIngredientPO>> ingredientMap = recipeIds.isEmpty()
                ? Map.of()
                : recipeIngredientMapper.selectByRecipeIds(recipeIds).stream()
                .collect(Collectors.groupingBy(RecipeIngredientPO::getRecipeId));
        Map<Long, List<RecipeStepPO>> stepMap = recipeIds.isEmpty()
                ? Map.of()
                : recipeStepMapper.selectByRecipeIds(recipeIds).stream()
                .collect(Collectors.groupingBy(RecipeStepPO::getRecipeId));

        List<RecipeVO> recipeVOs = records.stream().map(recipe -> {
            RecipeVO recipeVO = PoConvertToVo.convertToRecipeVO(recipe);
            recipeVO.setIngredients(ingredientMap.getOrDefault(recipe.getId(), List.of()).stream()
                    .map(PoConvertToVo::convertToIngredientVO)
                    .toList());
            recipeVO.setSteps(stepMap.getOrDefault(recipe.getId(), List.of()).stream()
                    .map(PoConvertToVo::convertToStepVO)
                    .toList());
            return recipeVO;
        }).toList();

        IPage<RecipeVO> pageResultVo = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        pageResultVo.setRecords(recipeVOs);
        return PageRecipeVO.of(pageResultVo);
    }

    @Override
    public ApiResponse<List<RecipeCategoryVO>> queryRecipeCategory() {
        List<RecipeCategoryPO> recipeCategoryPOS = recipeCategoryMapper.queryRecipeCategory();
        if (recipeCategoryPOS == null || recipeCategoryPOS.isEmpty()) {
            return ApiResponse.queryFail();
        }
        List<RecipeCategoryVO> recipeCategoryVOS = recipeCategoryPOS.stream()
                .map(PoConvertToVo::convertToRecipeCategoryVO)
                .toList();
        return ApiResponse.success(recipeCategoryVOS);
    }
}
