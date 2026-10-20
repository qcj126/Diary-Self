package diary.diarygoal.controller;

import diary.common.auth.CurrentUser;
import diary.diarygoal.entity.dto.GoalDashboardQueryDTO;
import diary.diarygoal.entity.vo.GoalDashboardVO;
import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import diary.diarygoal.service.dashboard.GoalDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/goal/dashboard")
public class GoalDashboardController {
    private final GoalDashboardService goalDashboardService;
    private final CurrentUser currentUser;

    @OperLog(module = "目标仪表盘", description = "查询目标周期复盘", operationType = "SELECT", saveResult = true)
    @PostMapping("/review")
    public ApiResponse<GoalDashboardVO> getReview(
            @RequestBody(required = false) GoalDashboardQueryDTO query) {
        return goalDashboardService.getReview(query, currentUser.getUser());
    }
}
