package diary.diarydiet.service.query;

import diary.common.entity.diet.dto.DietQueryDTO;
import diary.common.entity.diet.vo.DietRecordVO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface DietQueryService {
    ApiResponse<DietRecordVO> getDietRecordById(Long id);
    ApiResponse<List<DietRecordVO>> getDietRecordsByUserId(Long userId);
    ApiResponse<List<DietRecordVO>> queryDietRecords(DietQueryDTO query);
}
