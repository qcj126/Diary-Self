package diary.diarytimemachine.controller;

import diary.common.entity.timemachine.dto.TimeCardDTO;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;
import diary.common.result.ApiResponse;
import diary.diarytimemachine.service.add.TimeMachineAddService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("time-machine/")
public class TimeMachineController {
    @Resource
    private TimeMachineAddService timeMachineAddService;

    // 增
    @PostMapping("/category/add")
    public ApiResponse<Integer> addCategory(@RequestBody TimeCategoryDTO categoryDTO) {
        return ApiResponse.success(timeMachineAddService.categoryAdd(categoryDTO));
    }

    @PostMapping("/card/add")
    public ApiResponse<Integer> addCard(@RequestBody TimeCardDTO cardDTO) {
        return ApiResponse.success(timeMachineAddService.cardAdd(cardDTO));
    }

    // 删

    // 改

    // 查
}
