package diary.diarytimemachine.impl.delete;

import diary.common.convert.timemachine.DtoConvertToPo;
import diary.common.entity.timemachine.dto.TimeCardDTO;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;
import diary.common.entity.timemachine.po.TimeCardPO;
import diary.common.entity.timemachine.po.TimeCategoryPO;
import diary.common.exception.ParamIllegalException;
import diary.common.exception.SameDataException;
import diary.diarytimemachine.mapper.TimeMachineMapper;
import diary.diarytimemachine.service.add.TimeMachineAddService;
import diary.diarytimemachine.service.delete.TimeMachineDeleteService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TimeMachineDeleteServiceImpl implements TimeMachineDeleteService {
    @Resource
    private TimeMachineMapper timeMachineMapper;

    @Override
    public String categoryDelete(TimeCategoryDTO categoryDTO) {
        Integer i = timeMachineMapper.logicallyDeleteCategory(categoryDTO.getId());
        if (i > 0) {
            return "时光分类删除成功";
        }
        return "时光分类删除成功";
    }

    @Override
    public String cardDelete(TimeCardDTO cardDTO) {
        Integer i = timeMachineMapper.logicallyDeleteCard(cardDTO.getId());
        if (i > 0) {
            return "时光卡片删除成功";
        }
        return "时光卡片删除失败";
    }
}
