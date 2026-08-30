package diary.diarytimemachine.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import diary.common.entity.timemachine.dto.TimeCardDTO;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;
import diary.common.entity.timemachine.vo.TimeCardVO;
import diary.common.entity.timemachine.vo.TimeCategoryVO;
import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import diary.diarytimemachine.service.add.TimeMachineAddService;
import diary.diarytimemachine.service.delete.TimeMachineDeleteService;
import diary.diarytimemachine.service.query.TimeMachineQueryService;
import diary.diarytimemachine.service.update.TimeMachineUpdateService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("time-machine")
public class TimeMachineController {
    @Resource
    private TimeMachineAddService timeMachineAddService;
    @Resource
    private TimeMachineUpdateService timeMachineUpdateService;
    @Resource
    private TimeMachineDeleteService timeMachineDeleteService;
    @Resource
    private TimeMachineQueryService timeMachineQueryService;

    // 增
    @OperLog(module = "时光机", description = "新增时光机分类", operationType = "INSERT", saveResult = true)
    @PostMapping("/category/add")
    public ApiResponse<Integer> addCategory(@RequestBody TimeCategoryDTO categoryDTO) {
        return ApiResponse.success(timeMachineAddService.categoryAdd(categoryDTO));
    }

    @OperLog(module = "时光机", description = "新增时光卡片", operationType = "INSERT", saveResult = true)
    @PostMapping("/card/add")
    public ApiResponse<Integer> addCard(@RequestBody TimeCardDTO cardDTO) {
        return ApiResponse.success(timeMachineAddService.cardAdd(cardDTO));
    }

    // 删
    @OperLog(module = "时光机", description = "删除时光机分类", operationType = "DELETE", saveResult = true)
    @PostMapping("/category/delete")
    public ApiResponse<String> deleteCategory(@RequestBody TimeCategoryDTO categoryDTO) {
        return ApiResponse.success(timeMachineDeleteService.categoryDelete(categoryDTO));
    }

    @OperLog(module = "时光机", description = "删除时光卡片", operationType = "DELETE", saveResult = true)
    @PostMapping("/card/delete")
    public ApiResponse<String> deleteCard(@RequestBody TimeCardDTO cardDTO) {
        return ApiResponse.success(timeMachineDeleteService.cardDelete(cardDTO));
    }

    // 改
    @OperLog(module = "时光机", description = "修改时光机分类", operationType = "UPDATE", saveResult = true)
    @PostMapping("/category/update")
    public ApiResponse<String> updateCategory(@RequestBody TimeCategoryDTO categoryDTO) {
        return ApiResponse.success(timeMachineUpdateService.categoryUpdate(categoryDTO));
    }

    @OperLog(module = "时光机", description = "修改时光卡片", operationType = "UPDATE", saveResult = true)
    @PostMapping("/card/update")
    public ApiResponse<String> updateCard(@RequestBody TimeCardDTO cardDTO) {
        return ApiResponse.success(timeMachineUpdateService.cardUpdate(cardDTO));
    }

    // 查
    @OperLog(module = "时光机", description = "查询时光机分类", operationType = "SELECT", saveResult = true)
    @PostMapping("/category/query")
    public ApiResponse<List<TimeCategoryVO>> queryCategory() {
        return ApiResponse.success(timeMachineQueryService.categoryQuery());
    }

    @OperLog(module = "时光机", description = "分页查询时光卡片", operationType = "SELECT", saveResult = true)
    @PostMapping("/card/query")
    public ApiResponse<IPage<TimeCardVO>> queryCard(@RequestBody TimeCardDTO cardDTO) {
        return ApiResponse.success(timeMachineQueryService.cardQuery(cardDTO));
    }
}
