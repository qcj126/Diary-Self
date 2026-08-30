package diary.diarygoal.controller;

import diary.common.entity.goal.dto.GoalQueryDTO;
import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.entity.goal.vo.StageGoalVO;
import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import diary.diarygoal.service.add.GoalAddService;
import diary.diarygoal.service.delete.GoalDeleteService;
import diary.diarygoal.service.export.ExportService;
import diary.diarygoal.service.query.GoalQueryService;
import diary.diarygoal.service.update.GoalUpdateService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/goal")
public class GoalController {
    @Resource
    private GoalAddService goalAddService;
    @Resource
    private GoalDeleteService goalDeleteService;
    @Resource
    private GoalUpdateService goalUpdateService;
    @Resource
    private GoalQueryService goalQueryService;
    @Resource
    private ExportService exportService;

    @OperLog(module = "阶段目标", description = "新增阶段目标", operationType = "INSERT", saveResult = true)
    @PostMapping("/add")
    public ApiResponse<String> addGoal(@RequestBody StageGoalDTO stageGoalDTO) {
        return goalAddService.addGoal(stageGoalDTO);
    }

    @OperLog(module = "阶段目标", description = "批量新增子目标", operationType = "INSERT", saveResult = true)
    @PostMapping("/batch/addSubGoal")
    public ApiResponse<String> batchAddSubGoal(@RequestBody List<SubGoalDTO> subGoalDTOList) {
        return goalAddService.batchAddSubGoal(subGoalDTOList);
    }

    @OperLog(module = "阶段目标", description = "删除阶段目标", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteGoal(@PathVariable Long id) {
        return goalDeleteService.deleteGoal(id);
    }

    @OperLog(module = "阶段目标", description = "修改阶段目标", operationType = "UPDATE", saveResult = true)
    @PostMapping("/update")
    public ApiResponse<String> updateGoal(@RequestBody StageGoalDTO stageGoalDTO) {
        return goalUpdateService.updateGoal(stageGoalDTO);
    }

    @OperLog(module = "阶段目标", description = "查询阶段目标列表", operationType = "SELECT", saveResult = true)
    @PostMapping("/query")
    public ApiResponse<List<StageGoalVO>> queryGoals(@RequestBody(required = false) GoalQueryDTO goalQueryDTO) {
        return goalQueryService.queryGoals(goalQueryDTO);
    }

    @OperLog(module = "阶段目标", description = "查询阶段目标详情", operationType = "SELECT", saveResult = true)
    @GetMapping("/query/{id}")
    public ApiResponse<StageGoalVO> getGoalById(@PathVariable Long id) {
        return goalQueryService.getGoalById(id);
    }

    @OperLog(module = "阶段目标", description = "导出阶段目标", operationType = "EXPORT", saveResult = false)
    @PostMapping("/export")
    public ApiResponse<String> exportGoal(
            @RequestParam(defaultValue = "1") Integer exportType,
            @RequestParam(defaultValue = "7") Integer lastDays,
            @RequestParam(defaultValue = "10") Integer exportSize) {
        exportService.export(exportType, lastDays, exportSize);
        return ApiResponse.success("Export successful");
    }
}
