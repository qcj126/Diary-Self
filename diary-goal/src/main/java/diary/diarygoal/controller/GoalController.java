package diary.diarygoal.controller;

import diary.common.auth.CurrentUser;
import diary.diarygoal.entity.dto.GoalQueryDTO;
import diary.diarygoal.entity.dto.StageGoalDTO;
import diary.diarygoal.entity.dto.SubGoalDTO;
import diary.diarygoal.entity.vo.StageGoalVO;
import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import diary.diarygoal.service.add.GoalAddService;
import diary.diarygoal.service.delete.GoalDeleteService;
import diary.diarygoal.service.export.ExportService;
import diary.diarygoal.service.export.GoalExportFile;
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

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
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
    @Resource
    private CurrentUser currentUser;

    @OperLog(module = "阶段目标", description = "新增阶段目标", operationType = "INSERT", saveResult = true)
    @PostMapping("/add")
    public ApiResponse<String> addGoal(@RequestBody StageGoalDTO stageGoalDTO) {
        applyUser(stageGoalDTO);
        return goalAddService.addGoal(stageGoalDTO);
    }

    @OperLog(module = "阶段目标", description = "批量新增子目标", operationType = "INSERT", saveResult = true)
    @PostMapping("/batch/addSubGoal")
    public ApiResponse<String> batchAddSubGoal(@RequestBody List<SubGoalDTO> subGoalDTOList) {
        Long userId = currentUser.getUser();
        subGoalDTOList.forEach(item -> item.setUserId(userId));
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
        applyUser(stageGoalDTO);
        return goalUpdateService.updateGoal(stageGoalDTO);
    }

    @OperLog(module = "阶段目标", description = "查询阶段目标列表", operationType = "SELECT", saveResult = true)
    @PostMapping("/query")
    public ApiResponse<List<StageGoalVO>> queryGoals(@RequestBody(required = false) GoalQueryDTO goalQueryDTO) {
        if (goalQueryDTO == null) {
            goalQueryDTO = new GoalQueryDTO();
        }
        goalQueryDTO.setUserId(currentUser.getUser());
        return goalQueryService.queryGoals(goalQueryDTO);
    }

    @OperLog(module = "阶段目标", description = "查询阶段目标详情", operationType = "SELECT", saveResult = true)
    @GetMapping("/query/{id}")
    public ApiResponse<StageGoalVO> getGoalById(@PathVariable Long id) {
        return goalQueryService.getGoalById(id);
    }

    @OperLog(module = "阶段目标", description = "导出阶段目标", operationType = "EXPORT", saveResult = false)
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportGoal(
            @RequestParam(defaultValue = "1") Integer exportType,
            @RequestParam(defaultValue = "1") Integer ddl,
            @RequestParam(defaultValue = "1") Integer category,
            @RequestParam(defaultValue = "7") Integer lastDays,
            @RequestParam(defaultValue = "10") Integer exportSize) {
        Long userId = currentUser.getUser();
        validateExportParams(exportType, ddl, category, lastDays, exportSize);
        GoalExportFile file = exportService.export(userId, exportType, ddl, category, lastDays, exportSize);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(file.content());
    }

    private void applyUser(StageGoalDTO stageGoalDTO) {
        Long userId = currentUser.getUser();
        stageGoalDTO.setUserId(userId);
        if (stageGoalDTO.getSubGoals() != null) {
            stageGoalDTO.getSubGoals().forEach(item -> item.setUserId(userId));
        }
    }

    private void validateExportParams(Integer exportType, Integer ddl, Integer category,
                                      Integer lastDays, Integer exportSize) {
        if (exportType == null || exportType < 1 || exportType > 3) {
            throw new IllegalArgumentException("exportType 只能是 1、2 或 3");
        }
        if (ddl == null || ddl < 1 || ddl > 4) {
            throw new IllegalArgumentException("ddl 只能是 1 到 4");
        }
        if (category == null || category < 1 || category > 5) {
            throw new IllegalArgumentException("category 只能是 1 到 5");
        }
        if (lastDays == null || lastDays < 0 || lastDays > 3650) {
            throw new IllegalArgumentException("lastDays 必须在 0 到 3650 之间");
        }
        if (exportSize == null || exportSize < 1 || exportSize > 100) {
            throw new IllegalArgumentException("exportSize 必须在 1 到 100 之间");
        }
    }
}
