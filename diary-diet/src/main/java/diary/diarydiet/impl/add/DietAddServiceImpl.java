package diary.diarydiet.impl.add;

import diary.common.convert.diet.DTOConvertToPO;
import diary.common.entity.diet.dto.DietRecordDTO;
import diary.common.entity.diet.po.DietRecordPO;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import diary.diarydiet.mapper.DietMapper;
import diary.diarydiet.service.add.DietAddService;

import java.time.LocalDateTime;

@Service
public class DietAddServiceImpl implements DietAddService {
    @Resource
    private DietMapper dietMapper;

    @Override
    public ApiResponse<String> addDietRecord(DietRecordDTO dietRecordDTO) {
        if (dietRecordDTO == null) {
            throw new ParamIllegalException("入参为空");
        }

        // 参数校验
        if (dietRecordDTO.getUserId() == null ||
            dietRecordDTO.getEatTime() == null ||
            dietRecordDTO.getMealType() == null ||
            dietRecordDTO.getFoodName() == null ||
            dietRecordDTO.getCalories() == null) {
            throw new ParamIllegalException("必填参数不能为空");
        }

        // DTO转PO
        long primaryKey = MyUtils.getPrimaryKey();
        DietRecordPO dietRecordPO = DTOConvertToPO.dietRecordDTOConvertToPO(dietRecordDTO, primaryKey);

        // 插入数据库
        dietMapper.insert(dietRecordPO);
        return ApiResponse.success("饮食记录添加成功");
    }
}
