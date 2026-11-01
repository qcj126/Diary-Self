package diary.diarydiet.impl.add;

import diary.diarydiet.entity.convert.DTOConvertToPO;
import diary.diarydiet.entity.dto.DietRecordAddDTO;
import diary.diarydiet.entity.po.DietRecordPO;
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
    public ApiResponse<Long> addDietRecord(DietRecordAddDTO dietRecordAddDTO, Long userId) {
        DietRecordValidator.validateForAdd(dietRecordAddDTO, userId);

        long primaryKey = MyUtils.getPrimaryKey();
        DietRecordPO dietRecordPO = DTOConvertToPO.dietRecordAddDTOConvertToPO(
                dietRecordAddDTO, primaryKey, userId);
        dietRecordPO.setDeleted(false);

        if (dietMapper.insert(dietRecordPO) != 1) {
            return ApiResponse.fail(291, "添加失败");
        }
        return ApiResponse.success(primaryKey);
    }
}
