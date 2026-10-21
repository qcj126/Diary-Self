package diary.diaryrecipe.controller;

import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import diary.diaryrecipe.entity.dto.IngredientReqDto;
import diary.diaryrecipe.entity.vo.CookWayVo;
import diary.diaryrecipe.entity.vo.IngredientCategoryVo;
import diary.diaryrecipe.entity.vo.IngredientVO;
import diary.diaryrecipe.entity.vo.sys.IngredientVo;
import diary.diaryrecipe.service.sys.add.SysInfoAddService;
import diary.diaryrecipe.service.sys.query.SysInfoQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/recipe/sys")
@RequiredArgsConstructor
public class SysRecipeController {
    private final SysInfoQueryService sysInfoQueryService;

    private final SysInfoAddService sysInfoAddService;
    // 查询食材分类
    @OperLog(module = "系统数据", description = "查询食材分类", operationType = "SELECT", saveResult = true)
    @GetMapping("/ingredients/category")
    public ApiResponse<List<IngredientCategoryVo>> getIngredientCategory(@RequestParam Integer isMain) {
        return sysInfoQueryService.getIngredientCategories(isMain);
    }

    // 根据分类和是否主料查询食材，然后选择食材，并手动填写用量
    @OperLog(module = "系统数据", description = "按分类查询食材", operationType = "SELECT", saveResult = true)
    @PostMapping("/ingredients")
    public ApiResponse<List<IngredientVo>> getIngredientsByCategory(@RequestBody IngredientReqDto ingredientReqDto) {
        return sysInfoQueryService.getIngredientsByCategory(ingredientReqDto);
    }

    // 查询全部烹饪方式
    @OperLog(module = "系统数据", description = "查询烹饪方式", operationType = "SELECT", saveResult = true)
    @GetMapping("/cook-ways")
    public ApiResponse<List<CookWayVo>> getCookWays() {
        return sysInfoQueryService.getCookWays();
    }

    // 添加食材时也为食材添加图标（从现有图标库中选择）
    @OperLog(module = "系统数据", description = "新增食材", operationType = "INSERT", saveResult = true)
    @PostMapping("/ingredients/add")
    public ApiResponse<?> addIngredient(@RequestBody IngredientReqDto ingredientReqDto) {
        return sysInfoAddService.addIngredient(ingredientReqDto);
    }
}
