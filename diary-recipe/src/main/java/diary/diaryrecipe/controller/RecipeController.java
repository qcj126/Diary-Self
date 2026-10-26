package diary.diaryrecipe.controller;

import com.aliyun.core.annotation.Path;
import diary.common.auth.CurrentUser;
import diary.diaryrecipe.entity.dto.RecipeCategoryDto;
import diary.diaryrecipe.entity.dto.RecipePageReqDto;
import diary.diaryrecipe.entity.dto.RecipeReqDto;
import diary.diaryrecipe.entity.vo.PageRecipeVO;
import diary.diaryrecipe.entity.vo.RecipeCategoryVO;
import diary.diaryrecipe.entity.vo.RecipeVO;
import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import diary.diaryrecipe.service.add.RecipeAddService;
import diary.diaryrecipe.service.delete.RecipeDeleteService;
import diary.diaryrecipe.service.query.RecipeQueryService;
import diary.diaryrecipe.service.update.RecipeUpdateService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    @Resource
    private CurrentUser currentUser;

    // 增加食谱
    @OperLog(module = "食谱", description = "新增食谱", operationType = "INSERT", saveResult = true)
    @PostMapping("/add")
    public ApiResponse<String> addRecipe(@RequestBody RecipeReqDto recipeReqDto) {
        recipeReqDto.setAuthorId(currentUser.getUser());
        return recipeAddService.addRecipe(recipeReqDto);
    }

    // 分页查询食谱
    @OperLog(module = "食谱", description = "分页查询食谱", operationType = "SELECT", saveResult = true)
    @PostMapping("/query")
    public ApiResponse<PageRecipeVO<RecipeVO>> pageQueryRecipe(@RequestBody RecipePageReqDto recipePageReqDto) {
        return recipeQueryService.pageQueryRecipe(recipePageReqDto);
    }

    // 查询单条食谱
    @OperLog(module = "食谱", description = "分页查询食谱", operationType = "SELECT", saveResult = true)
    @GetMapping("/query/{recipeId}")
    public ApiResponse<String> queryRecipeTitleById(@PathVariable("recipeId") Long recipeId) {
        Long userId = currentUser.getUser();
        return recipeQueryService.queryRecipe(userId, recipeId);
    }

    // 修改食谱
    @OperLog(module = "食谱", description = "修改食谱", operationType = "UPDATE", saveResult = true)
    @PostMapping("/update")
    public ApiResponse<String> updateRecipe(@RequestBody RecipeReqDto recipeReqDto) {
        recipeReqDto.setAuthorId(currentUser.getUser());
        return recipeUpdateService.updateRecipe(recipeReqDto);
    }

    // 删除食谱
    @OperLog(module = "食谱", description = "删除食谱", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete")
    public ApiResponse<String> deleteRecipe(@RequestBody RecipeReqDto recipeReqDto) {
        recipeReqDto.setAuthorId(currentUser.getUser());
        return recipeDeleteService.deleteRecipe(recipeReqDto);
    }

    // 查询食谱分类
    @OperLog(module = "食谱", description = "查询食谱分类", operationType = "SELECT", saveResult = true)
    @PostMapping("/query/category")
    public ApiResponse<List<RecipeCategoryVO>> queryCategory() {
        return recipeQueryService.queryRecipeCategory();
    }

    // 删除食谱分类
    @OperLog(module = "食谱", description = "删除食谱分类", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete/category")
    public ApiResponse<String> deleteCategory(@RequestBody RecipeCategoryDto recipeCategoryDto) {
        recipeCategoryDto.setUserId(currentUser.getUser());
        return recipeDeleteService.deleteCategory(recipeCategoryDto);
    }

    // 添加食谱分类
    @OperLog(module = "食谱", description = "新增食谱分类", operationType = "INSERT", saveResult = true)
    @PostMapping("/add/category")
    public ApiResponse<String> addCategory(@RequestBody RecipeCategoryDto recipeCategoryDto) {
        recipeCategoryDto.setUserId(currentUser.getUser());
        return recipeAddService.addCategory(recipeCategoryDto);
    }

    // 修改食谱分类
    @OperLog(module = "食谱", description = "修改食谱分类", operationType = "UPDATE", saveResult = true)
    @PostMapping("/update/category")
    public ApiResponse<String> updateCategory(@RequestBody RecipeCategoryDto recipeCategoryDto) {
        recipeCategoryDto.setUserId(currentUser.getUser());
        return recipeUpdateService.updateCategory(recipeCategoryDto);
    }
}
