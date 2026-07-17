package diary.diarydiet.impl.update;

import diary.common.convert.diet.DTOConvertToPO;
import diary.common.entity.diet.dto.DietRecordDTO;
import diary.common.entity.diet.po.DietRecordPO;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import diary.diarydiet.mapper.DietMapper;
import diary.diarydiet.service.update.DietUpdateService;

@Service
public class DietUpdateServiceImpl implements DietUpdateService {
    @Resource
    private DietMapper dietMapper;

    @Override
    public ApiResponse<String> updateDietRecord(DietRecordDTO dietRecordDTO) {
        if (dietRecordDTO == null) {
            return ApiResponse.fail(400, "入参为空");
        }

        // 参数校验
        if (dietRecordDTO.getId() == null) {
            throw new ParamIllegalException("记录ID不能为空");
        }

        // 查询记录是否存在
        DietRecordPO existRecord = dietMapper.selectById(dietRecordDTO.getId());
        if (existRecord == null) {
            throw new ParamIllegalException("饮食记录不存在");
        }

        // 更新字段
        DietRecordPO dietRecordPO = DTOConvertToPO.dietRecordDTOConvertToPO(dietRecordDTO, dietRecordDTO.getId());

        // 更新数据库
        dietMapper.updateById(dietRecordPO);

        return ApiResponse.success("饮食记录更新成功");
    }
}
