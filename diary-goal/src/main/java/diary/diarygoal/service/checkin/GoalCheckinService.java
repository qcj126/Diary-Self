package diary.diarygoal.service.checkin;

import diary.common.entity.goal.dto.GoalCheckinDTO;
import diary.common.entity.goal.dto.GoalCheckinQueryDTO;
import diary.common.entity.goal.vo.GoalCheckinVO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface GoalCheckinService {
    ApiResponse<GoalCheckinVO> addCheckin(GoalCheckinDTO request, Long userId);

    ApiResponse<GoalCheckinVO> updateCheckin(GoalCheckinDTO request, Long userId);

    ApiResponse<String> deleteCheckin(Long id, Long userId);

    ApiResponse<GoalCheckinVO> getCheckin(Long id, Long userId);

    ApiResponse<List<GoalCheckinVO>> queryCheckins(GoalCheckinQueryDTO query, Long userId);
}
