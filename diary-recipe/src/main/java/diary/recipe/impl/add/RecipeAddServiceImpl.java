package diary.recipe.impl.add;

import diary.common.result.ApiResponse;
import diary.common.entity.recipe.ao.RecipeIngredientAO;
import diary.common.entity.recipe.ao.RecipeStepAO;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
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
            throw new RuntimeException("食谱存在必填参数为空");
        }
        // 同一作者、同一食谱分类下，不能存在相同食谱名称
        if (recipeMapper.selectByAuthorTitle(recipeReqDto.getAuthorId(), recipeReqDto.getTitle(), recipeReqDto.getMealType()) > 0) {
            throw new RuntimeException("作者在该分类下已存在同名食谱");
        }

        List<RecipeIngredientAO> ingredients = recipeReqDto.getIngredients();
        ingredients.stream().filter(ingredient -> ingredient.getName() == null || ingredient.getQuantity() == null)
            .findAny().ifPresent(ingredient -> {throw new RuntimeException("食材存在必填参数为空");});

        List<RecipeStepAO> steps = recipeReqDto.getSteps();
        steps.stream().filter(step -> step.getDescription() == null || step.getStepNumber() == null)
            .findAny().ifPresent(step -> {throw new RuntimeException("步骤存在必填参数为空");});

        RecipePO recipePO = new RecipePO();
        List<RecipeIngredientPO> recipeIngredientPOs = new ArrayList<>();
        List<RecipeStepPO> recipeStepPOs = new ArrayList<>();
        // 填充元素
        recipePO.setRecipeId(MyUtils.getPrimaryKey());
        recipePO.setAuthorId(recipeReqDto.getAuthorId());
        recipePO.setTitle(recipeReqDto.getTitle());
        recipePO.setCoverImg(recipeReqDto.getCoverImg());
        recipePO.setDescription(recipeReqDto.getDescription());
        recipePO.setCategory(recipeReqDto.getCategory());
        recipePO.setMealType(recipeReqDto.getMealType());
        recipePO.setDifficulty(recipeReqDto.getDifficulty());
        recipePO.setCookingTime(recipeReqDto.getCookingTime());
        recipePO.setStory(recipeReqDto.getStory());
        recipePO.setIsAnniversary(recipeReqDto.getIsAnniversary());
        recipePO.setAnniversaryDate(recipeReqDto.getAnniversaryDate());
        recipePO.setStatus(recipeReqDto.getStatus()); // 已发布
        recipePO.setViewCount(0);
        recipePO.setLikeCount(0);
        recipePO.setCookCount(0);
        recipePO.setIsAnniversary(recipeReqDto.getIsAnniversary());
        recipePO.setAnniversaryDate(recipeReqDto.getAnniversaryDate());
        recipePO.setCreatedAt(LocalDateTime.now());
        recipePO.setUpdatedAt(LocalDateTime.now());

        for (RecipeIngredientAO ao : ingredients) {
            RecipeIngredientPO po = new RecipeIngredientPO();
            po.setIngredientId(MyUtils.getPrimaryKey());
            po.setRecipeId(recipePO.getRecipeId());
            po.setName(ao.getName());
            po.setQuantity(ao.getQuantity());
            po.setIsMain(ao.getIsMain());
            po.setSortOrder(ao.getSortOrder());
            recipeIngredientPOs.add(po);
        }

        for (RecipeStepAO ao : steps) {
            RecipeStepPO po = new RecipeStepPO();
            po.setStepId(MyUtils.getPrimaryKey());
            po.setRecipeId(recipePO.getRecipeId());
            po.setStepNumber(ao.getStepNumber());
            po.setDescription(ao.getDescription());
            po.setImageUrl(ao.getImageUrl());
            po.setTimerMin(ao.getTimerMin());
            po.setSortOrder(ao.getSortOrder());
            recipeStepPOs.add(po);
        }

        recipeMapper.insert(recipePO);
        recipeIngredientMapper.batchInsert(recipeIngredientPOs);
        recipeStepMapper.batchInsert(recipeStepPOs);
        
        return ApiResponse.success("食谱添加成功");
    }
}
