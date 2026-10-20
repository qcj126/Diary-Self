package diary.diarygoal.service.checkin;

import diary.diarygoal.entity.dto.GoalCheckinDTO;
import diary.diarygoal.entity.dto.GoalCheckinQueryDTO;
import diary.diarygoal.entity.vo.GoalCheckinVO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface GoalCheckinService {
    ApiResponse<GoalCheckinVO> addCheckin(GoalCheckinDTO request, Long userId);

    ApiResponse<GoalCheckinVO> updateCheckin(GoalCheckinDTO request, Long userId);

    ApiResponse<String> deleteCheckin(Long id, Long userId);

    ApiResponse<GoalCheckinVO> getCheckin(Long id, Long userId);

    ApiResponse<List<GoalCheckinVO>> queryCheckins(GoalCheckinQueryDTO query, Long userId);
}
