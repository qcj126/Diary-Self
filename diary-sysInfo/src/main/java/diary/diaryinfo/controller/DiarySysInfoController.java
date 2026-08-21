package diary.diaryinfo.controller;

import diary.common.entity.sysInfo.dto.IngredientReqDto;
import diary.common.entity.sysInfo.vo.CookWayVo;
import diary.common.entity.sysInfo.vo.IngredientCategoryVo;
import diary.common.entity.sysInfo.vo.IngredientVo;
import diary.common.result.ApiResponse;
import diary.diaryinfo.service.SysInfoQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sys/info")
@RequiredArgsConstructor
public class DiarySysInfoController {
    private final SysInfoQueryService sysInfoQueryService;

    // 查询食材分类
    @GetMapping("/ingredients/category")
    public ApiResponse<List<IngredientCategoryVo>> getIngredientCategory(@RequestParam Integer isMain) {
        return sysInfoQueryService.getIngredientCategories(isMain);
    }

    // 根据分类和是否主料查询食材，然后选择食材，并手动填写用量
    @PostMapping("/ingredients")
    public ApiResponse<List<IngredientVo>> getIngredientsByCategory(@RequestBody IngredientReqDto ingredientReqDto) {
        return sysInfoQueryService.getIngredientsByCategory(ingredientReqDto);
    }

    // 查询全部烹饪方式
    @GetMapping("/cook-ways")
    public ApiResponse<List<CookWayVo>> getCookWays() {
        return sysInfoQueryService.getCookWays();
    }
}
