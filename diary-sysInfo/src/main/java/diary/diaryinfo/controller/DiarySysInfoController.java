package diary.diaryinfo.controller;

import diary.common.entity.file.dto.IconDTO;
import diary.common.entity.love.dto.LoveMoodDTO;
import diary.common.entity.love.dto.LoveRecordMoodDTO;
import diary.common.entity.love.dto.LoveRecordTagDTO;
import diary.common.entity.love.dto.LoveTagDTO;
import diary.common.entity.love.vo.LoveMoodVO;
import diary.common.entity.love.vo.LoveTagVO;
import diary.common.entity.sysInfo.dto.IngredientReqDto;
import diary.common.entity.sysInfo.vo.CookWayVo;
import diary.common.entity.sysInfo.vo.IngredientCategoryVo;
import diary.common.entity.sysInfo.vo.IngredientVo;
import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import diary.diaryinfo.service.addservice.DiaryLoveAddService;
import diary.diaryinfo.service.addservice.IconAddService;
import diary.diaryinfo.service.addservice.SysInfoAddService;
import diary.diaryinfo.service.deleteservice.DiaryLoveDeleteService;
import diary.diaryinfo.service.deleteservice.IconDeleteService;
import diary.diaryinfo.service.queryservice.DiaryLoveQueryService;
import diary.diaryinfo.service.queryservice.IconQueryService;
import diary.diaryinfo.service.queryservice.SysInfoQueryService;
import diary.diaryinfo.service.updateservice.DiaryLoveUpdateService;
import diary.diaryinfo.service.updateservice.IconUpdateService;
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
    private final DiaryLoveAddService diaryLoveAddService;
    private final DiaryLoveQueryService diaryLoveQueryService;
    private final DiaryLoveDeleteService diaryLoveDeleteService;
    private final DiaryLoveUpdateService diaryLoveUpdateService;

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

    @OperLog(module = "系统数据", description = "上传系统图标", operationType = "UPLOAD", saveParams = false, saveResult = true)
    @PostMapping("/icon/add")
    public ApiResponse<?> addIcon(@RequestParam("file") MultipartFile file,
                                  @ModelAttribute IconDTO iconDTO) {
        return iconAddService.addIcon(file, iconDTO);
    }

    @OperLog(module = "系统数据", description = "查询系统图标", operationType = "SELECT", saveResult = true)
    @PostMapping("/icon/query")
    public ApiResponse<?> queryIcons(@RequestBody(required = false) IconDTO iconDTO) {
        return iconQueryService.queryIcons(iconDTO);
    }

    @OperLog(module = "系统数据", description = "修改系统图标", operationType = "UPDATE", saveParams = false, saveResult = true)
    @PostMapping("/icon/update")
    public ApiResponse<?> updateIcon(@RequestParam(value = "file", required = false) MultipartFile file,
                                     @ModelAttribute IconDTO iconDTO) {
        return iconUpdateService.updateIcon(file, iconDTO);
    }

    @OperLog(module = "系统数据", description = "删除系统图标", operationType = "DELETE", saveResult = true)
    @PostMapping("/icon/delete")
    public ApiResponse<?> deleteIcon(@RequestBody IconDTO iconDTO) {
        return iconDeleteService.deleteIcon(iconDTO);
    }

    // 前端发起请求，获取静态图片
    @OperLog(module = "系统数据", description = "获取系统图标文件", operationType = "DOWNLOAD", saveResult = false)
    @GetMapping(value = "/icon/{fileName}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getIcon(@PathVariable String fileName) throws IOException {
        return iconQueryService.getIcon(fileName);
    }

    @OperLog(module = "系统数据", description = "新增心情", operationType = "INSERT", saveResult = true)
    @PostMapping("/add/moods")
    public ApiResponse<String> addMood(@RequestBody LoveMoodDTO dto) {
        return diaryLoveAddService.addMood(dto);
    }

    @OperLog(module = "系统数据", description = "新增标签", operationType = "INSERT", saveResult = true)
    @PostMapping("/add/tags")
    public ApiResponse<String> addTag(@RequestBody LoveTagDTO dto) {
        return diaryLoveAddService.addTag(dto);
    }

    @OperLog(module = "系统数据", description = "修改心情", operationType = "UPDATE", saveResult = true)
    @PostMapping("/update/moods")
    public ApiResponse<String> updateMood(@RequestBody LoveMoodDTO dto) {
        return diaryLoveUpdateService.updateMood(dto);
    }

    @OperLog(module = "系统数据", description = "修改标签", operationType = "UPDATE", saveResult = true)
    @PostMapping("/update/tags")
    public ApiResponse<String> updateTag(@RequestBody LoveTagDTO dto) {
        return diaryLoveUpdateService.updateTag(dto);
    }

    @OperLog(module = "系统数据", description = "查询心情", operationType = "SELECT", saveResult = true)
    @GetMapping("/query/moods")
    public ApiResponse<List<LoveMoodVO>> queryMoods(@RequestParam(required = false) Boolean enabled) {
        return diaryLoveQueryService.queryMoods(enabled);
    }

    @OperLog(module = "系统数据", description = "查询标签", operationType = "SELECT", saveResult = true)
    @GetMapping("/query/tags/{coupleId}")
    public ApiResponse<List<LoveTagVO>> queryTags(@PathVariable Long coupleId) {
        return diaryLoveQueryService.queryTags(coupleId);
    }

    @OperLog(module = "系统数据", description = "删除心情", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete/moods/{id}")
    public ApiResponse<String> deleteMood(@PathVariable Long id) {
        return diaryLoveDeleteService.deleteMood(id);
    }

    @OperLog(module = "系统数据", description = "删除标签", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete/tags/{id}")
    public ApiResponse<String> deleteTag(@PathVariable Long id) {
        return diaryLoveDeleteService.deleteTag(id);
    }
}
