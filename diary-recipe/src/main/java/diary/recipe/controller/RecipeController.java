package diary.recipe.controller;

import diary.common.entity.recipe.dto.req.RecipePageReqDto;
import diary.common.entity.recipe.dto.resp.PageRespDto;
import diary.common.entity.recipe.dto.resp.RecipeRespDto;
import diary.common.result.ApiResponse;
import diary.common.entity.recipe.dto.req.RecipeReqDto;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import diary.recipe.service.add.RecipeAddService;
import diary.recipe.service.delete.RecipeDeleteService;
import diary.recipe.service.query.RecipeQueryService;
import diary.recipe.service.update.RecipeUpdateService;

import java.util.Map;

@RestController
@RequestMapping("/recipe")
public class RecipeController {
    @Resource
    private RecipeAddService recipeAddService;
    @Resource
    private RecipeQueryService recipeQueryService;
    @Resource
    private RecipeUpdateService recipeUpdateService;
    @Resource
    private RecipeDeleteService recipeDeleteService;

    // 增加食谱
    @PostMapping("/add")
    public ApiResponse<String> addRecipe(@RequestBody RecipeReqDto recipeReqDto) {
        return recipeAddService.addRecipe(recipeReqDto);
    }

    // 分页查询食谱
    @PostMapping("/query")
    public ApiResponse<PageRespDto<RecipeRespDto>> pageQueryRecipe(@RequestBody RecipePageReqDto recipePageReqDto) {
        PageRespDto<RecipeRespDto> pagedQueryRecipe = recipeQueryService.pageQueryRecipe(recipePageReqDto);
        return ApiResponse.success(pagedQueryRecipe);
    }

    // 修改食谱
    @PostMapping("/update")
    public ApiResponse<Map<String, Object>> updateRecipe(@RequestBody RecipeReqDto recipeReqDto) {
        return recipeUpdateService.updateRecipe(recipeReqDto);
    }
    // 删除食谱
    @PostMapping("/delete")
    public ApiResponse<Map<String, Object>> deleteRecipe(@RequestBody RecipeReqDto recipeReqDto) {
        return recipeDeleteService.deleteRecipe(recipeReqDto);
    }
}
