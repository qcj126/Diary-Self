package diary.diarylove.controller;

import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.vo.LoveCoupleVO;
import diary.common.result.ApiResponse;
import diary.diarylove.service.DiaryLoveService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/love")
@RequiredArgsConstructor
public class DiaryLoveController {
    private final DiaryLoveService diaryLoveService;

    /**
     * 创建情侣关系
     */
    @PostMapping("/add/couples")
    public ApiResponse<String> addCouples(@RequestBody LoveCoupleDTO loveCoupleDTO) {
        return diaryLoveService.addCouples(loveCoupleDTO) > 0
                ? ApiResponse.success("添加情侣关系成功") : ApiResponse.addFail();
    }
}
