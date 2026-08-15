package diary.diaryai.controller;

import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.vo.AiTaskResultVo;
import diary.common.entity.ai.vo.AiTaskStatusVo;
import diary.common.entity.ai.vo.AiTaskSubmitVo;
import diary.common.result.ApiResponse;
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

    @PostMapping("/task")
    public ResponseEntity<ApiResponse<AiTaskSubmitVo>> invokeAiTasks (@RequestBody AiInvokeDTO aiInvokeDTO) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(aiTaskApplicationService.submitTask(aiInvokeDTO)));
    }

    @GetMapping("/task/{taskId}")
    public ApiResponse<AiTaskStatusVo> getAiTaskStatus (@PathVariable String taskId) {
        return ApiResponse.success(aiTaskQueryService.getTaskStatus(taskId));
    }

    @GetMapping("/task/result/{taskId}")
    public ApiResponse<AiTaskResultVo> getAiTaskResult (@PathVariable String taskId) {
        return ApiResponse.success(aiTaskQueryService.getTaskResult(taskId));
    }
}