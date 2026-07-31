package diary.diaryai.controller;

import diary.common.entity.ai.dto.AIInvokeDTO;
import diary.common.result.ApiResponse;
import diary.diaryai.service.CallAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class DiaryAIController {
    private final CallAIService callAIService;

    @PostMapping("/invoke")
    public ApiResponse<String> invokeAI (@RequestBody AIInvokeDTO aiInvokeDTO) {
        callAIService.callAI(aiInvokeDTO);
        return ApiResponse.success("调用AI成功，数据已处理");
    }
}
