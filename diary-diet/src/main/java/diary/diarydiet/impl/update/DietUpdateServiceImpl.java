package diary.diarydiet.impl.update;

import diary.common.convert.diet.DTOConvertToPO;
import diary.common.entity.diet.dto.DietRecordDTO;
import diary.common.entity.diet.po.DietRecordPO;
import diary.common.result.ApiResponse;
import diary.diarydiet.support.DietRecordValidator;
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
        DietRecordValidator.validateForUpdate(dietRecordDTO);

        DietRecordPO existRecord = dietMapper.selectById(dietRecordDTO.getId());
        if (existRecord == null) {
            return ApiResponse.updateFail();
        }

        DietRecordPO dietRecordPO = DTOConvertToPO.dietRecordDTOConvertToPO(dietRecordDTO, dietRecordDTO.getId());
        if (dietMapper.updateById(dietRecordPO) != 1) {
            return ApiResponse.updateFail();
        }

        return ApiResponse.success("饮食记录更新成功");
    }
}
