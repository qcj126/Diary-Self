package qcj.diary.diarydiet.service.query;

import diary.common.entity.diet.vo.DietRecordVO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface DietQueryService {
    ApiResponse<DietRecordVO> getDietRecordById(Long id);
    ApiResponse<List<DietRecordVO>> getDietRecordsByUserId(Long userId);
}
