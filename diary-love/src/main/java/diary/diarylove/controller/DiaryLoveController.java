package diary.diarylove.controller;

import diary.common.entity.love.dto.*;
import diary.common.entity.love.vo.LoveAnniversaryVO;
import diary.common.entity.love.vo.LoveCoupleVO;
import diary.common.entity.love.vo.LoveLocationVO;
import diary.common.entity.love.vo.LoveRecordImageVO;
import diary.common.entity.love.vo.LoveRecordVO;
import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import diary.diarylove.service.DiaryLoveAddService;
import diary.diarylove.service.DiaryLoveDeleteService;
import diary.diarylove.service.DiaryLoveQueryService;
import diary.diarylove.service.DiaryLoveUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/love")
@RequiredArgsConstructor
public class DiaryLoveController {
    private final DiaryLoveAddService diaryLoveAddService;
    private final DiaryLoveQueryService diaryLoveQueryService;
    private final DiaryLoveDeleteService diaryLoveDeleteService;
    private final DiaryLoveUpdateService diaryLoveUpdateService;

    @OperLog(module = "恋爱记录", description = "创建情侣关系", operationType = "INSERT", saveResult = true)
    @PostMapping("/add/couples") // 已测
    public ApiResponse<String> addCouples(@RequestBody LoveCoupleDTO dto) {
        return diaryLoveAddService.addCouples(dto);
    }

    @OperLog(module = "恋爱记录", description = "新增纪念日", operationType = "INSERT", saveResult = true)
    @PostMapping("/add/anniversaries") // 已测
    public ApiResponse<String> addAnniversary(@RequestBody LoveAnniversaryDTO dto) {
        return diaryLoveAddService.addAnniversary(dto);
    }

    @OperLog(module = "恋爱记录", description = "新增恋爱记录", operationType = "INSERT", saveResult = true)
    @PostMapping("/add/records") // 已测
    public ApiResponse<String> addRecord(@Valid @RequestBody AddLoveRecordDto dto) {
        return diaryLoveAddService.addRecord(dto);
    }

    @OperLog(module = "恋爱记录", description = "查询情侣关系", operationType = "SELECT", saveResult = true)
    @PostMapping("/query/couples") // 已测
    public ApiResponse<LoveCoupleVO> queryCouples(@RequestBody LoveCoupleDTO loveCoupleDTO) {
        return diaryLoveQueryService.queryCouples(loveCoupleDTO);
    }

    @OperLog(module = "恋爱记录", description = "查询纪念日", operationType = "SELECT", saveResult = true)
    @GetMapping("/query/anniversaries/{coupleId}") // 已测
    public ApiResponse<List<LoveAnniversaryVO>> queryAnniversaries(@PathVariable Long coupleId) {
        return diaryLoveQueryService.queryAnniversaries(coupleId);
    }

    @OperLog(module = "恋爱记录", description = "查询地点", operationType = "SELECT", saveResult = true)
    @GetMapping("/query/locations/{coupleId}") // 已测
    public ApiResponse<List<LoveLocationVO>> queryLocations(@PathVariable Long coupleId) {
        return diaryLoveQueryService.queryLocations(coupleId);
    }

    @OperLog(module = "恋爱记录", description = "查询恋爱记录详情", operationType = "SELECT", saveResult = true)
    @GetMapping("/query/records/{id}") // 已测
    public ApiResponse<LoveRecordVO> queryRecord(@PathVariable Long id) {
        return diaryLoveQueryService.queryRecord(id);
    }

    @OperLog(module = "恋爱记录", description = "查询恋爱记录列表", operationType = "SELECT", saveResult = true)
    @PostMapping("/query/records") // 已测
    public ApiResponse<List<LoveRecordVO>> queryRecords(@RequestBody LoveRecordDTO query) {
        return diaryLoveQueryService.queryRecords(query);
    }

    @OperLog(module = "恋爱记录", description = "查询记录图片", operationType = "SELECT", saveResult = true)
    @GetMapping("/query/record-images/{recordId}") // 已测
    public ApiResponse<List<LoveRecordImageVO>> queryRecordImages(@PathVariable Long recordId) {
        return diaryLoveQueryService.queryRecordImages(recordId);
    }

    @OperLog(module = "恋爱记录", description = "修改情侣关系", operationType = "UPDATE", saveResult = true)
    @PostMapping("/update/couples") // 已测
    public ApiResponse<String> updateCouple(@RequestBody LoveCoupleDTO dto) {
        return diaryLoveUpdateService.updateCouple(dto);
    }

    @OperLog(module = "恋爱记录", description = "修改纪念日", operationType = "UPDATE", saveResult = true)
    @PostMapping("/update/anniversaries") // 已测
    public ApiResponse<String> updateAnniversary(@RequestBody LoveAnniversaryDTO dto) {
        return diaryLoveUpdateService.updateAnniversary(dto);
    }

    @OperLog(module = "恋爱记录", description = "修改地点", operationType = "UPDATE", saveResult = true)
    @PostMapping("/update/locations") // 已测
    public ApiResponse<String> updateLocation(@RequestBody LoveLocationDTO dto) {
        return diaryLoveUpdateService.updateLocation(dto);
    }

    @OperLog(module = "恋爱记录", description = "修改恋爱记录", operationType = "UPDATE", saveResult = true)
    @PostMapping("/update/records") // 已测
    public ApiResponse<String> updateRecord(@Valid @RequestBody UpdateLoveRecordDto dto) {
        return diaryLoveUpdateService.updateRecord(dto);
    }

    @OperLog(module = "恋爱记录", description = "修改记录图片关联", operationType = "UPDATE", saveResult = true)
    @PostMapping("/update/record-images")
    public ApiResponse<String> updateRecordImage(@RequestBody LoveRecordImageDTO dto) {
        return diaryLoveUpdateService.updateRecordImage(dto);
    }

    @OperLog(module = "恋爱记录", description = "删除情侣关系", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete/couples/{id}")
    public ApiResponse<String> deleteCouple(@PathVariable Long id) {
        return diaryLoveDeleteService.deleteCouple(id);
    }

    @OperLog(module = "恋爱记录", description = "删除纪念日", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete/anniversaries/{id}")
    public ApiResponse<String> deleteAnniversary(@PathVariable Long id) {
        return diaryLoveDeleteService.deleteAnniversary(id);
    }

    @OperLog(module = "恋爱记录", description = "删除地点", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete/locations/{id}")
    public ApiResponse<String> deleteLocation(@PathVariable Long id) {
        return diaryLoveDeleteService.deleteLocation(id);
    }

    @OperLog(module = "恋爱记录", description = "删除恋爱记录", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete/records/{id}") // 已测
    public ApiResponse<String> deleteRecord(@PathVariable Long id) {
        return diaryLoveDeleteService.deleteRecord(id);
    }

    @OperLog(module = "恋爱记录", description = "删除记录图片关联", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete/record-images/{id}") // 已测
    public ApiResponse<String> deleteRecordImage(@PathVariable Long id) {
        return diaryLoveDeleteService.deleteRecordImage(id);
    }
}
