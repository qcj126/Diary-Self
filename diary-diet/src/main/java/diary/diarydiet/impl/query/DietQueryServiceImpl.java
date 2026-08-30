package diary.diarydiet.impl.query;

import diary.common.convert.diet.POConvertToVO;
import diary.common.entity.diet.dto.DietQueryDTO;
import diary.common.entity.diet.po.DietRecordPO;
import diary.common.entity.diet.vo.DietRecordVO;
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
}
