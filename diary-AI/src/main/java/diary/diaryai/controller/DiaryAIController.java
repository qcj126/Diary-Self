package diary.diaryai.controller;

import diary.common.auth.CurrentUser;
import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.vo.AiTaskResultVo;
import diary.common.entity.ai.vo.AiTaskStatusVo;
import diary.common.entity.ai.vo.AiTaskSubmitVo;
import diary.common.enums.aienum.AiTaskStatusEnum;
import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import diary.diaryai.service.AiTaskApplicationService;
import diary.diaryai.service.AiTaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class DiaryAIController {
    private final AiTaskApplicationService aiTaskApplicationService;
    private final AiTaskQueryService aiTaskQueryService;
    private final CurrentUser currentUser;

    @OperLog(module = "AI", description = "提交AI任务", operationType = "INSERT", saveParams = false, saveResult = false)
    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<AiTaskSubmitVo>> submit(@RequestBody AiInvokeDTO request) {
        AiTaskSubmitVo submitted = aiTaskApplicationService.submitTask(request, currentUser.getUser());
        return ResponseEntity.accepted()
                .location(URI.create("/ai/tasks/" + submitted.getTaskId()))
                .body(ApiResponse.success(submitted));
    }

    @OperLog(module = "AI", description = "获取AI任务状态", operationType = "SELECT", saveParams = true, saveResult = true)
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<AiTaskStatusVo>> status(@PathVariable Long taskId) {
        return ResponseEntity.ok(ApiResponse.success(
                aiTaskQueryService.getTaskStatus(taskId, currentUser.getUser())));
    }

    @OperLog(module = "AI", description = "获取AI任务结果", operationType = "SELECT", saveParams = true, saveResult = false)
    @GetMapping("/tasks/{taskId}/result")
    public ResponseEntity<ApiResponse<AiTaskResultVo>> result(@PathVariable Long taskId) {
        AiTaskResultVo result = aiTaskQueryService.getTaskResult(taskId, currentUser.getUser());
        HttpStatus status = AiTaskStatusEnum.valueOf(result.getStatus()).isTerminal()
                ? HttpStatus.OK
                : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status)
                .body(ApiResponse.success(result));
    }
    @OperLog(module = "AI", description = "查看AI任务列表", operationType = "SELECT", saveParams = true, saveResult = false)
    @GetMapping("/tasks/query/list")
    public ApiResponse<List<AiTaskResultVo>> list(
            @RequestParam(required = false) String taskStatus,
            @RequestParam(required = false) Integer applicationCode,
            @RequestParam(required = false) String title,
            @RequestParam(required = false, defaultValue = "50") Integer pageSize,
            @RequestParam(required = false, defaultValue = "0") Integer pageNum
    ) {
        return ApiResponse.success(aiTaskQueryService.getAiTaskList(currentUser.getUser(), taskStatus, applicationCode, title, pageSize, pageNum));
    }
}
