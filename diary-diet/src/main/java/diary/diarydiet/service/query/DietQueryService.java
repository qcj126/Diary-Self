package diary.diarydiet.service.query;

import diary.diarydiet.entity.dto.DietQueryDTO;
import diary.diarydiet.entity.vo.DietRecordVO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface DietQueryService {
    ApiResponse<DietRecordVO> getDietRecordById(Long id);
    ApiResponse<List<DietRecordVO>> getDietRecordsByUserId(Long userId);
    ApiResponse<List<DietRecordVO>> queryDietRecords(DietQueryDTO query);
    ApiResponse<String> queryFoodName(Long dietId, Long userId);
}
