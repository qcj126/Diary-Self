package diary.diarydiet.impl.delete;

import diary.common.entity.diet.po.DietRecordPO;
import diary.common.result.ApiResponse;
import diary.diarydiet.support.DietRecordValidator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import diary.diarydiet.mapper.DietMapper;
import diary.diarydiet.service.delete.DietDeleteService;

@Service
public class DietDeleteServiceImpl implements DietDeleteService {
    @Resource
    private DietMapper dietMapper;

    @Override
    public ApiResponse<String> deleteDietRecord(Long id) {
        DietRecordValidator.validateId(id);

        DietRecordPO dietRecordPO = dietMapper.selectById(id);
        if (dietRecordPO == null) {
            return ApiResponse.delFail();
        }

        if (dietMapper.logicalDeleteById(id) != 1) {
            return ApiResponse.delFail();
        }
        return ApiResponse.success("删除成功");
    }
}
