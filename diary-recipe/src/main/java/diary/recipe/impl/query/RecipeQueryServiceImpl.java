package diary.recipe.impl.query;

import diary.common.entity.recipe.ao.AuthorInfoAO;
import diary.common.entity.recipe.dto.req.PageReqDto;
import diary.common.entity.recipe.dto.req.RecipePageReqDto;
import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.entity.recipe.dto.resp.PageRespDto;
import diary.common.entity.recipe.dto.resp.RecipeRespDto;
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

    @Override
    public PageRespDto<RecipeRespDto> pageQueryRecipe(RecipePageReqDto pageReqDto) {

        return null;
    }
}
