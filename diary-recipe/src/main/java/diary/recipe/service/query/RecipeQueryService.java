package diary.recipe.service.query;

import diary.common.entity.recipe.dto.req.RecipePageReqDto;
import diary.common.entity.recipe.dto.resp.PageRespDto;
import diary.common.entity.recipe.vo.RecipeVO;

public interface RecipeQueryService {
    PageRespDto<RecipeVO> pageQueryRecipe(RecipePageReqDto pageReqDto);
}
