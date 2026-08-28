package diary.diaryai.controller;

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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class DiaryAIController {
    private final AiTaskApplicationService aiTaskApplicationService;
    private final AiTaskQueryService aiTaskQueryService;

    @OperLog(module = "AI", description = "提交AI任务", operationType = "INSERT", saveParams = true, saveResult = false)
    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<AiTaskSubmitVo>> submit(
            @RequestBody AiInvokeDTO request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(aiTaskApplicationService.submitTask(request)));
    }

    @OperLog(module = "AI", description = "获取AI任务状态", operationType = "SELECT", saveParams = true, saveResult = true)
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<AiTaskStatusVo>> status(
            @PathVariable Long taskId) {
        return ResponseEntity.ok(
                ApiResponse.success(aiTaskQueryService.getTaskStatus(taskId)));
    }

    @OperLog(module = "AI", description = "获取AI任务结果", operationType = "SELECT", saveParams = true, saveResult = false)
    @GetMapping("/tasks/{taskId}/result")
    public ResponseEntity<ApiResponse<AiTaskResultVo>> result(
            @PathVariable Long taskId) {
        AiTaskResultVo result = aiTaskQueryService.getTaskResult(taskId);
        HttpStatus status = AiTaskStatusEnum.SUCCESS.name().equals(result.getStatus())
                ? HttpStatus.OK
                : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status)
                .body(ApiResponse.success(result));
    }
}