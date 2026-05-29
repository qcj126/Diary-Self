package qcj.diary.diarydiet.controller;

import diary.common.entity.diet.dto.DietRecordDTO;
import diary.common.entity.diet.vo.DietRecordVO;
import diary.common.result.ApiResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import qcj.diary.diarydiet.service.add.DietAddService;
import qcj.diary.diarydiet.service.delete.DietDeleteService;
import qcj.diary.diarydiet.service.query.DietQueryService;
import qcj.diary.diarydiet.service.update.DietUpdateService;

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
    @PostMapping("/add")
    public ApiResponse<String> addDietRecord(@RequestBody DietRecordDTO dietRecordDTO) {
        return dietAddService.addDietRecord(dietRecordDTO);
    }

    // 删
    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteDietRecord(@PathVariable Long id) {
        return dietDeleteService.deleteDietRecord(id);
    }

    // 改
    @PostMapping("/update")
    public ApiResponse<String> updateDietRecord(@RequestBody DietRecordDTO dietRecordDTO) {
        return dietUpdateService.updateDietRecord(dietRecordDTO);
    }

    // 查 - 根据ID查询
    @GetMapping("/query/{id}")
    public ApiResponse<DietRecordVO> getDietRecordById(@PathVariable Long id) {
        return dietQueryService.getDietRecordById(id);
    }

    // 查 - 根据用户ID查询所有记录
    @GetMapping("/query/user/{userId}")
    public ApiResponse<List<DietRecordVO>> getDietRecordsByUserId(@PathVariable Long userId) {
        return dietQueryService.getDietRecordsByUserId(userId);
    }
}
