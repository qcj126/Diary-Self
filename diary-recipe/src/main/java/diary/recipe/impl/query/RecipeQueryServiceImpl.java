package diary.recipe.impl.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import diary.recipe.entity.convert.DtoConvertToVo;
import diary.recipe.entity.convert.PoConvertToVo;
import diary.recipe.entity.dto.db.RecipeCategoryIconDto;
import diary.recipe.entity.dto.req.RecipePageReqDto;
import diary.recipe.entity.vo.PageRecipeVO;
import diary.recipe.entity.po.RecipeIngredientPO;
import diary.recipe.entity.po.RecipePO;
import diary.recipe.entity.po.RecipeStepPO;
import diary.recipe.entity.vo.RecipeCategoryVO;
import diary.recipe.entity.vo.RecipeVO;
import diary.common.result.ApiResponse;
import diary.recipe.mapper.RecipeCategoryMapper;
import diary.recipe.mapper.RecipeIngredientMapper;
import diary.recipe.mapper.RecipeMapper;
import diary.recipe.mapper.RecipeStepMapper;
import diary.recipe.service.query.RecipeQueryService;
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
    public ApiResponse<PageRecipeVO<RecipeVO>> pageQueryRecipe(RecipePageReqDto recipePageReqDto) {
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
        if (records == null || records.isEmpty()) {
            return ApiResponse.queryFail();
        }
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
        return ApiResponse.success(PageRecipeVO.of(pageResultVo));
    }

    @Override
    public ApiResponse<List<RecipeCategoryVO>> queryRecipeCategory() {
        List<RecipeCategoryIconDto> recipeCategoryIconDtos = recipeCategoryMapper.queryRecipeCategory();
        if (recipeCategoryIconDtos == null || recipeCategoryIconDtos.isEmpty()) {
            return ApiResponse.queryFail();
        }
        List<RecipeCategoryVO> recipeCategoryVOS = recipeCategoryIconDtos.stream()
                .map(DtoConvertToVo::convertToRecipeCategoryVO)
                .toList();
        return ApiResponse.success(recipeCategoryVOS);
    }
}
