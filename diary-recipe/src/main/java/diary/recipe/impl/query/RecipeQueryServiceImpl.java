package diary.recipe.impl.query;

import diary.common.entity.recipe.ao.AuthorInfoAO;
import diary.common.entity.recipe.dto.RecipeReqDto;
import diary.common.entity.recipe.po.RecipeIngredientPO;
import diary.common.entity.recipe.po.RecipePO;
import diary.common.entity.recipe.po.RecipeStepPO;
import diary.common.entity.recipe.vo.IngredientVO;
import diary.common.entity.recipe.vo.RecipeVO;
import diary.common.entity.recipe.vo.StepVO;
import diary.common.entity.user.po.User;
import diary.common.result.ApiResponse;
import diary.dao.mapper.recipe.RecipeIngredientMapper;
import diary.dao.mapper.recipe.RecipeMapper;
import diary.dao.mapper.recipe.RecipeStepMapper;
import diary.dao.mapper.user.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import diary.recipe.service.query.RecipeQueryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecipeQueryServiceImpl implements RecipeQueryService {
    @Resource
    private RecipeMapper recipeMapper;

    @Resource
    private RecipeIngredientMapper recipeIngredientMapper;

    @Resource
    private RecipeStepMapper recipeStepMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public ApiResponse<Map<String, Object>> queryRecipe(RecipeReqDto recipeReqDto) {
        if (recipeReqDto == null) {
            throw new RuntimeException("参数不能为空");
        }

        if (recipeReqDto.getRecipeId() == null) {
            throw new RuntimeException("食谱id不能为空");
        }

        RecipePO recipePO = recipeMapper.selectById(recipeReqDto.getRecipeId());
        List<RecipeIngredientPO> recipeIngredientPOs = recipeIngredientMapper.selectByRecipeId(recipeReqDto.getRecipeId());
        List<RecipeStepPO> recipeStepPOs = recipeStepMapper.selectByRecipeId(recipeReqDto.getRecipeId());
        User author = userMapper.selectByUserId(recipePO.getAuthorId());
        Long userId = author.getUserId();
        String username = author.getUsername();
        AuthorInfoAO authorInfoAO = new AuthorInfoAO();
        authorInfoAO.setUserId(userId);
        authorInfoAO.setNickname(username);

        RecipeVO recipeVO = new RecipeVO();
        List<IngredientVO> ingredientVOs = new ArrayList<>();
        List<StepVO> stepVOs = new ArrayList<>();
        for (RecipeIngredientPO recipeIngredientPO : recipeIngredientPOs) {
            IngredientVO IngredientVO = new IngredientVO();
            BeanUtils.copyProperties(recipeIngredientPO, IngredientVO);
            ingredientVOs.add(IngredientVO);
        }

        for (RecipeStepPO recipeStepPO : recipeStepPOs) {
            StepVO stepVO = new StepVO();
            BeanUtils.copyProperties(recipeStepPO, stepVO);
            stepVOs.add(stepVO);
        }
        BeanUtils.copyProperties(recipePO, recipeVO);
        recipeVO.setIngredients(ingredientVOs);
        recipeVO.setSteps(stepVOs);
        recipeVO.setAuthor(authorInfoAO);
        Map<String, Object> result = new HashMap<>();
        result.put("recipe", recipeVO);

        return ApiResponse.success(result);
    }
}
