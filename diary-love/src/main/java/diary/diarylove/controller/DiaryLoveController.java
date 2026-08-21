package diary.diarylove.controller;

import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.vo.LoveCoupleVO;
import diary.common.result.ApiResponse;
import diary.diarylove.service.DiaryLoveAddService;
import diary.diarylove.service.DiaryLoveDeleteService;
import diary.diarylove.service.DiaryLoveQueryService;
import diary.diarylove.service.DiaryLoveUpdateService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/love")
@RequiredArgsConstructor
public class DiaryLoveController {
    private final DiaryLoveAddService diaryLoveAddService;
    private final DiaryLoveQueryService diaryLoveQueryService;
    private final DiaryLoveDeleteService diaryLoveDeleteService;
    private final DiaryLoveUpdateService diaryLoveUpdateService;

    /**
     * 创建情侣关系
     */
    @PostMapping("/add/couples")
    public ApiResponse<String> addCouples(@RequestBody LoveCoupleDTO loveCoupleDTO) {
        return diaryLoveAddService.addCouples(loveCoupleDTO);
    }

    /**
     * 查询情侣关系
     *
     */
    @PostMapping("/query/couples")
    public ApiResponse<LoveCoupleVO> queryCouples(@RequestBody LoveCoupleDTO loveCoupleDTO) {
        return diaryLoveQueryService.queryCouples(loveCoupleDTO);
    }
}
