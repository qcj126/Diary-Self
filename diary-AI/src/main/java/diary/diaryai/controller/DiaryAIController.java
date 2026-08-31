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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class DiaryAIController {
    private final AiTaskApplicationService aiTaskApplicationService;
    private final AiTaskQueryService aiTaskQueryService;

    @OperLog(module = "AI", description = "提交AI任务", operationType = "INSERT", saveParams = false, saveResult = false)
    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<AiTaskSubmitVo>> submit(
            @RequestBody AiInvokeDTO request,
            @RequestHeader("X-Auth-User-Id") Long userId) {
        AiTaskSubmitVo submitted = aiTaskApplicationService.submitTask(request, userId);
        return ResponseEntity.accepted()
                .location(URI.create("/ai/tasks/" + submitted.getTaskId()))
                .body(ApiResponse.success(submitted));
    }

    @OperLog(module = "AI", description = "获取AI任务状态", operationType = "SELECT", saveParams = true, saveResult = true)
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<AiTaskStatusVo>> status(
            @PathVariable Long taskId,
            @RequestHeader("X-Auth-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(aiTaskQueryService.getTaskStatus(taskId, userId)));
    }

    @OperLog(module = "AI", description = "获取AI任务结果", operationType = "SELECT", saveParams = true, saveResult = false)
    @GetMapping("/tasks/{taskId}/result")
    public ResponseEntity<ApiResponse<AiTaskResultVo>> result(
            @PathVariable Long taskId,
            @RequestHeader("X-Auth-User-Id") Long userId) {
        AiTaskResultVo result = aiTaskQueryService.getTaskResult(taskId, userId);
        /*
         * 改前：只有 SUCCESS 返回 200，FAILED/DEAD_LETTER 等终态仍永久返回 202。
         * 改后：所有终态都返回 200，只有仍在处理的状态返回 202；业务成功/失败由响应体 status 表达。
         */
        HttpStatus status = AiTaskStatusEnum.valueOf(result.getStatus()).isTerminal()
                ? HttpStatus.OK
                : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status)
                .body(ApiResponse.success(result));
    }
}
