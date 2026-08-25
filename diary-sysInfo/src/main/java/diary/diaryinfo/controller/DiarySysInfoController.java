package diary.diaryinfo.controller;

import diary.common.entity.file.dto.IconDTO;
import diary.common.entity.sysInfo.dto.IngredientReqDto;
import diary.common.entity.sysInfo.vo.CookWayVo;
import diary.common.entity.sysInfo.vo.IngredientCategoryVo;
import diary.common.entity.sysInfo.vo.IngredientVo;
import diary.common.result.ApiResponse;
import diary.diaryinfo.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/sys/info")
@RequiredArgsConstructor
public class DiarySysInfoController {
    private final SysInfoQueryService sysInfoQueryService;
    private final SysInfoAddService sysInfoAddService;
    private final IconAddService iconAddService;
    private final IconDeleteService iconDeleteService;
    private final IconQueryService iconQueryService;
    private final IconUpdateService iconUpdateService;

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

    // 添加食材时也为食材添加图标（从现有图标库中选择）
    @PostMapping("/ingredients/add")
    public ApiResponse<?> addIngredient(@RequestBody IngredientReqDto ingredientReqDto) {
        return sysInfoAddService.addIngredient(ingredientReqDto);
    }

    @PostMapping("/icon/add")
    public ApiResponse<?> addIcon(@RequestParam("file") MultipartFile file,
                                  @ModelAttribute IconDTO iconDTO) {
        return iconAddService.addIcon(file, iconDTO);
    }

    @PostMapping("/icon/query")
    public ApiResponse<?> queryIcons(@RequestBody(required = false) IconDTO iconDTO) {
        return iconQueryService.queryIcons(iconDTO);
    }

    @PostMapping("/icon/update")
    public ApiResponse<?> updateIcon(@RequestParam(value = "file", required = false) MultipartFile file,
                                     @ModelAttribute IconDTO iconDTO) {
        return iconUpdateService.updateIcon(file, iconDTO);
    }

    @PostMapping("/icon/delete")
    public ApiResponse<?> deleteIcon(@RequestBody IconDTO iconDTO) {
        return iconDeleteService.deleteIcon(iconDTO);
    }

    // 前端发起请求，获取静态图片
    @GetMapping(value = "/icon/{fileName}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getIcon(@PathVariable String fileName) throws IOException {
        return iconQueryService.getIcon(fileName);
    }
}
