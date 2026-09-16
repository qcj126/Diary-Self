package diary.diarygoal.controller;

import diary.common.auth.CurrentUser;
import diary.common.entity.goal.dto.GoalCheckinDTO;
import diary.common.entity.goal.dto.GoalCheckinQueryDTO;
import diary.common.entity.goal.vo.GoalCheckinVO;
import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import diary.diarygoal.service.checkin.GoalCheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/goal/checkin")
public class GoalCheckinController {
    private final GoalCheckinService goalCheckinService;
    private final CurrentUser currentUser;

    @OperLog(module = "目标打卡", description = "新增目标打卡", operationType = "INSERT", saveResult = true)
    @PostMapping("/add")
    public ApiResponse<GoalCheckinVO> addCheckin(@RequestBody GoalCheckinDTO request) {
        return goalCheckinService.addCheckin(request, currentUser.getUser());
    }

    @OperLog(module = "目标打卡", description = "修改目标打卡", operationType = "UPDATE", saveResult = true)
    @PostMapping("/update")
    public ApiResponse<GoalCheckinVO> updateCheckin(@RequestBody GoalCheckinDTO request) {
        return goalCheckinService.updateCheckin(request, currentUser.getUser());
    }

    @OperLog(module = "目标打卡", description = "删除目标打卡", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteCheckin(@PathVariable Long id) {
        return goalCheckinService.deleteCheckin(id, currentUser.getUser());
    }

    @OperLog(module = "目标打卡", description = "查询目标打卡详情", operationType = "SELECT", saveResult = true)
    @GetMapping("/query/{id}")
    public ApiResponse<GoalCheckinVO> getCheckin(@PathVariable Long id) {
        return goalCheckinService.getCheckin(id, currentUser.getUser());
    }

    @OperLog(module = "目标打卡", description = "查询目标打卡列表", operationType = "SELECT", saveResult = true)
    @PostMapping("/query")
    public ApiResponse<List<GoalCheckinVO>> queryCheckins(
            @RequestBody(required = false) GoalCheckinQueryDTO query) {
        return goalCheckinService.queryCheckins(query, currentUser.getUser());
    }
}
