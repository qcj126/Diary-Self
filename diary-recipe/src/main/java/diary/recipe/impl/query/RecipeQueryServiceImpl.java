package diary.recipe.impl.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import diary.common.entity.recipe.dto.req.RecipePageReqDto;

import diary.common.entity.recipe.dto.resp.PageRespDto;
import diary.common.entity.recipe.dto.resp.RecipeRespDto;

import diary.common.entity.recipe.po.RecipePO;
import diary.dao.mapper.recipe.RecipeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import diary.recipe.service.query.RecipeQueryService;

@Service
public class RecipeQueryServiceImpl implements RecipeQueryService {
    @Resource
    private RecipeMapper recipeMapper;

    @Override
    public PageRespDto<RecipeRespDto> pageQueryRecipe(RecipePageReqDto recipePageReqDto) {
        // 若参数全部为空，则默认查询最新的n条数据
        IPage<RecipePO> page = new Page<>(recipePageReqDto.getPageNum(), recipePageReqDto.getPageSize());
        IPage<RecipePO> pageResult = recipeMapper.qryPage(page, recipePageReqDto);
        IPage<RecipeRespDto> pageResultDto = new Page<>();
        for (RecipePO recipe : pageResult.getRecords()) {
            RecipeRespDto recipeRespDto = RecipeRespDto.fromEntity(recipe);
            pageResultDto.getRecords().add(recipeRespDto);
        }
        return PageRespDto.of(pageResultDto);
    }
}
