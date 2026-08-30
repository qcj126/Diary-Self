package diary.diarydiet.controller;

import diary.common.entity.diet.dto.DietQueryDTO;
import diary.common.entity.diet.dto.DietRecordDTO;
import diary.common.entity.diet.vo.DietRecordVO;
import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import diary.diarydiet.service.add.DietAddService;
import diary.diarydiet.service.delete.DietDeleteService;
import diary.diarydiet.service.query.DietQueryService;
import diary.diarydiet.service.update.DietUpdateService;

import java.util.List;

@RestController
@RequestMapping("/diet")
public class DietController {
    @Resource
    private DietAddService dietAddService;
    @Resource
    private DietDeleteService dietDeleteService;
    @Resource
    private DietUpdateService dietUpdateService;
    @Resource
    private DietQueryService dietQueryService;

    // 增
    @OperLog(module = "饮食记录", description = "新增饮食记录", operationType = "INSERT", saveResult = true)
    @PostMapping("/add")
    public ApiResponse<String> addDietRecord(@RequestBody DietRecordDTO dietRecordDTO) {
        return dietAddService.addDietRecord(dietRecordDTO);
    }

    // 删
    @OperLog(module = "饮食记录", description = "删除饮食记录", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteDietRecord(@PathVariable Long id) {
        return dietDeleteService.deleteDietRecord(id);
    }

    // 改
    @OperLog(module = "饮食记录", description = "修改饮食记录", operationType = "UPDATE", saveResult = true)
    @PostMapping("/update")
    public ApiResponse<String> updateDietRecord(@RequestBody DietRecordDTO dietRecordDTO) {
        return dietUpdateService.updateDietRecord(dietRecordDTO);
    }

    // 查 - 根据ID查询
    @OperLog(module = "饮食记录", description = "查询饮食记录详情", operationType = "SELECT", saveResult = true)
    @GetMapping("/query/{id}")
    public ApiResponse<DietRecordVO> getDietRecordById(@PathVariable Long id) {
        return dietQueryService.getDietRecordById(id);
    }

    // 查 - 根据用户ID查询所有记录
    @OperLog(module = "饮食记录", description = "按用户查询饮食记录", operationType = "SELECT", saveResult = true)
    @GetMapping("/query/user/{userId}")
    public ApiResponse<List<DietRecordVO>> getDietRecordsByUserId(@PathVariable Long userId) {
        return dietQueryService.getDietRecordsByUserId(userId);
    }

    // 查 - 支持用户、关键字、时间范围、餐别和地点组合查询
    @OperLog(module = "饮食记录", description = "组合查询饮食记录", operationType = "SELECT", saveResult = true)
    @PostMapping("/query")
    public ApiResponse<List<DietRecordVO>> queryDietRecords(@RequestBody DietQueryDTO query) {
        return dietQueryService.queryDietRecords(query);
    }
}
