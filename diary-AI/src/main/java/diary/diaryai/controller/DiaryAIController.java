package diary.diaryai.controller;

import diary.common.result.ApiResponse;
import diary.diaryai.service.CallAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class DiaryAIController {
    private final CallAIService callAIService;

    @PostMapping("/invoke")
    public ApiResponse<Object> invokeAI (@RequestParam Integer code,
                                         @RequestParam Integer id,
                                         @RequestParam Integer type) {
        return ApiResponse.success(callAIService.callAI(code, id, type));
    }
}
