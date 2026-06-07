package diary.diarydiet.impl.delete;

import diary.common.entity.diet.po.DietRecordPO;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
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
        if (id == null) {
            throw new ParamIllegalException("记录ID不能为空");
        }

        // 查询记录是否存在
        DietRecordPO dietRecordPO = dietMapper.selectById(id);
        if (dietRecordPO == null) {
            throw new ParamIllegalException("饮食记录不存在");
        }

        // 逻辑删除
        DietRecordPO updatePO = new DietRecordPO();
        updatePO.setId(id);
        updatePO.setDeleted(true);
        dietMapper.updateById(updatePO);

        return ApiResponse.success("删除成功");
    }
}
