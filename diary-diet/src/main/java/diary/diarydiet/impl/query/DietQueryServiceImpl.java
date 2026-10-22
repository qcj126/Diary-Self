package diary.diarydiet.impl.query;

import diary.diarydiet.entity.convert.POConvertToVO;
import diary.diarydiet.entity.dto.DietQueryDTO;
import diary.diarydiet.entity.po.DietRecordPO;
import diary.diarydiet.entity.vo.DietRecordVO;
import diary.common.result.ApiResponse;
import diary.diarydiet.support.DietRecordValidator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import diary.diarydiet.mapper.DietMapper;
import diary.diarydiet.service.query.DietQueryService;

import java.util.List;

@Service
public class DietQueryServiceImpl implements DietQueryService {
    @Resource
    private DietMapper dietMapper;

    @Override
    public ApiResponse<DietRecordVO> getDietRecordById(Long id) {
        DietRecordValidator.validateId(id);

        DietRecordPO dietRecordPO = dietMapper.selectById(id);
        if (dietRecordPO == null) {
            return ApiResponse.queryFail();
        }

        return ApiResponse.success(POConvertToVO.convertToVO(dietRecordPO));
    }

    @Override
    public ApiResponse<List<DietRecordVO>> getDietRecordsByUserId(Long userId) {
        DietQueryDTO query = new DietQueryDTO();
        query.setUserId(userId);
        return queryDietRecords(query);
    }

    @Override
    public ApiResponse<List<DietRecordVO>> queryDietRecords(DietQueryDTO query) {
        DietRecordValidator.validateQuery(query);
        if (query.getKeyword() != null) {
            query.setKeyword(query.getKeyword().trim());
        }
        List<DietRecordVO> records = dietMapper.selectList(query).stream()
                .map(POConvertToVO::convertToVO)
                .toList();
        return ApiResponse.success(records);
    }

    @Override
    public ApiResponse<String> queryFoodName(Long dietId, Long userId) {
        return ApiResponse.success(dietMapper.selectFoodNameByIdAndUserId(dietId, userId));
    }
}
