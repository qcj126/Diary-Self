package diary.diarygoal.controller;

import diary.common.entity.goal.dto.GoalQueryDTO;
import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.vo.StageGoalVO;
import diary.common.result.ApiResponse;
import diary.diarygoal.service.add.GoalAddService;
import diary.diarygoal.service.delete.GoalDeleteService;
import diary.diarygoal.service.query.GoalQueryService;
import diary.diarygoal.service.update.GoalUpdateService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/goal")
public class GoalController {
    @Resource
    private GoalAddService goalAddService;
    @Resource
    private GoalDeleteService goalDeleteService;
    @Resource
    private GoalUpdateService goalUpdateService;
    @Resource
    private GoalQueryService goalQueryService;

    @PostMapping("/add")
    public ApiResponse<String> addGoal(@RequestBody StageGoalDTO stageGoalDTO) {
        return goalAddService.addGoal(stageGoalDTO);
    }

    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteGoal(@PathVariable Long id) {
        return goalDeleteService.deleteGoal(id);
    }

    @PostMapping("/update")
    public ApiResponse<String> updateGoal(@RequestBody StageGoalDTO stageGoalDTO) {
        return goalUpdateService.updateGoal(stageGoalDTO);
    }

    @PostMapping("/query")
    public ApiResponse<List<StageGoalVO>> queryGoals(@RequestBody(required = false) GoalQueryDTO goalQueryDTO) {
        return goalQueryService.queryGoals(goalQueryDTO);
    }

    @GetMapping("/query/{id}")
    public ApiResponse<StageGoalVO> getGoalById(@PathVariable Long id) {
        return goalQueryService.getGoalById(id);
    }
}
