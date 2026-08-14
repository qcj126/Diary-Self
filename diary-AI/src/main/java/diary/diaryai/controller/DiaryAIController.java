package diary.diaryai.controller;

import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.vo.AiTaskSubmitVo;
import diary.common.result.ApiResponse;
import diary.diaryai.service.AiTaskApplicationService;
import diary.diaryai.service.CallAIService;
import diary.utils.commonutil.MyUtils;
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

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class DiaryAIController {
    private final AiTaskApplicationService aiTaskApplicationService;

    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<AiTaskSubmitVo>> invokeAiTasks (@RequestBody AiInvokeDTO aiInvokeDTO) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(aiTaskApplicationService.submitTask(aiInvokeDTO)));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<AiTaskSubmitVo>> getAiTaskStatus (@PathVariable String taskId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(aiTaskApplicationService.getTaskStatus(taskId)));
    }

    @GetMapping("/tasks/result/{taskId}")
    public ResponseEntity<ApiResponse<String>> getAiTaskResult (@PathVariable String taskId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(aiTaskApplicationService.getTaskResult(taskId)));
    }
}
