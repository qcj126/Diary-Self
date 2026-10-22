package diary.diaryrecipe.impl.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import diary.common.exception.ParamIllegalException;
import diary.diaryrecipe.entity.convert.DtoConvertToVo;
import diary.diaryrecipe.entity.convert.PoConvertToVo;
import diary.diaryrecipe.entity.dto.RecipeCategoryIconDto;
import diary.diaryrecipe.entity.dto.RecipePageReqDto;
import diary.diaryrecipe.entity.vo.PageRecipeVO;
import diary.diaryrecipe.entity.po.RecipeIngredientPO;
import diary.diaryrecipe.entity.po.RecipePO;
import diary.diaryrecipe.entity.po.RecipeStepPO;
import diary.diaryrecipe.entity.vo.RecipeCategoryVO;
import diary.diaryrecipe.entity.vo.RecipeVO;
import diary.common.result.ApiResponse;
import diary.diaryrecipe.mapper.RecipeCategoryMapper;
import diary.diaryrecipe.mapper.RecipeIngredientMapper;
import diary.diaryrecipe.mapper.RecipeMapper;
import diary.diaryrecipe.mapper.RecipeStepMapper;
import diary.diaryrecipe.service.query.RecipeQueryService;
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

    @Override
    public ApiResponse<String> queryRecipe(Long userId, Long recipeId) {
        if (recipeId == null || recipeId < 0 || userId == null || userId < 0) {
            throw new ParamIllegalException("非法参数！");
        }
        return ApiResponse.success(recipeMapper.selectTitleByUserIdAndRecipeId(userId, recipeId));
    }
}
