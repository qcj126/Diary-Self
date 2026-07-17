package diary.recipe.impl.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import diary.common.convert.recipe.PoConvertToVo;
import diary.common.entity.recipe.dto.req.RecipePageReqDto;

import diary.common.entity.recipe.dto.resp.PageRespDto;
import diary.common.entity.recipe.vo.RecipeVO;

import diary.common.entity.recipe.po.RecipePO;
import diary.common.exception.NullResultException;
import diary.recipe.mapper.RecipeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import diary.recipe.service.query.RecipeQueryService;

@Service
public class RecipeQueryServiceImpl implements RecipeQueryService {
    @Resource
    private RecipeMapper recipeMapper;

    @Override
    public PageRespDto<RecipeVO> pageQueryRecipe(RecipePageReqDto recipePageReqDto) {
        // 若参数全部为空，则默认查询最新的n条数据
        IPage<RecipePO> page = new Page<>(recipePageReqDto.getPageNum(), recipePageReqDto.getPageSize());
        IPage<RecipePO> pageResult = recipeMapper.qryPage(page, recipePageReqDto);
        if (pageResult == null) {
            throw new NullResultException("未查出相关数据");
        }
        IPage<RecipeVO> pageResultVo = new Page<>();
        for (RecipePO recipe : pageResult.getRecords()) {
            RecipeVO recipeVo = PoConvertToVo.convertToRecipeVO(recipe);
            pageResultVo.getRecords().add(recipeVo);
        }
        return PageRespDto.of(pageResultVo);
    }
}
