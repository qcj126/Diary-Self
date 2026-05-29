package qcj.diary.diarydiet.impl.query;

import diary.common.convert.diet.POConvertToVO;
import diary.common.entity.diet.po.DietRecordPO;
import diary.common.entity.diet.vo.DietRecordVO;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import qcj.diary.diarydiet.mapper.DietMapper;
import qcj.diary.diarydiet.service.query.DietQueryService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DietQueryServiceImpl implements DietQueryService {
    @Resource
    private DietMapper dietMapper;

    @Override
    public ApiResponse<DietRecordVO> getDietRecordById(Long id) {
        if (id == null) {
            throw new ParamIllegalException("记录ID不能为空");
        }

        DietRecordPO dietRecordPO = dietMapper.selectById(id);
        if (dietRecordPO == null) {
            throw new ParamIllegalException("饮食记录不存在");
        }

        return ApiResponse.success(POConvertToVO.convertToVO(dietRecordPO));
    }

    @Override
    public ApiResponse<List<DietRecordVO>> getDietRecordsByUserId(Long userId) {
        if (userId == null) {
            throw new ParamIllegalException("用户ID不能为空");
        }

        // 查询该用户的所有未删除的饮食记录
        List<DietRecordPO> dietRecordPOs = dietMapper.selectByUserId(userId);

        List<DietRecordVO> dietRecordVOs = dietRecordPOs.stream()
                                                       .map(POConvertToVO::convertToVO)
                                                       .collect(Collectors.toList());

        return ApiResponse.success(dietRecordVOs);
    }
}
