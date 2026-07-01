package diary.diarygoal.impl.export;

import diary.common.entity.goal.dto.StageGoalDTO;
import diary.diarygoal.mapper.GoalMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ExportServiceImpl {
    @Resource
    private GoalMapper goalMapper;

    public StageGoalDTO exportGoals(Integer exportType, Integer lastDays, Integer exportSize) {
        // TODO 从数据库中查数据，封装到 StageGoalDTO 中
        StageGoalDTO StageGoalDTO = new StageGoalDTO();
        return StageGoalDTO;
    }
}
