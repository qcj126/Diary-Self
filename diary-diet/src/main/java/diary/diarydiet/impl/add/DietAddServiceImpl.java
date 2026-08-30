package diary.diarydiet.impl.add;

import diary.common.convert.diet.DTOConvertToPO;
import diary.common.entity.diet.dto.DietRecordDTO;
import diary.common.entity.diet.po.DietRecordPO;
import diary.common.result.ApiResponse;
import diary.diarydiet.support.DietRecordValidator;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import diary.diarydiet.mapper.DietMapper;
import diary.diarydiet.service.add.DietAddService;

@Service
public class DietAddServiceImpl implements DietAddService {
    @Resource
    private DietMapper dietMapper;

    @Override
    public ApiResponse<String> addDietRecord(DietRecordDTO dietRecordDTO) {
        DietRecordValidator.validateForAdd(dietRecordDTO);

        long primaryKey = MyUtils.getPrimaryKey();
        DietRecordPO dietRecordPO = DTOConvertToPO.dietRecordDTOConvertToPO(dietRecordDTO, primaryKey);
        dietRecordPO.setDeleted(false);

        if (dietMapper.insert(dietRecordPO) != 1) {
            return ApiResponse.addFail();
        }
        return ApiResponse.success("饮食记录添加成功");
    }
}
