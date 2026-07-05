package diary.diarytimemachine.impl.update;

import diary.common.convert.timemachine.DtoConvertToPo;
import diary.common.entity.timemachine.dto.TimeCardDTO;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;
import diary.common.entity.timemachine.po.TimeCardPO;
import diary.common.entity.timemachine.po.TimeCategoryPO;
import diary.common.exception.ParamIllegalException;
import diary.common.exception.SameDataException;
import diary.diarytimemachine.mapper.TimeMachineMapper;
import diary.diarytimemachine.service.add.TimeMachineAddService;
import diary.diarytimemachine.service.update.TimeMachineUpdateService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TimeMachineUpdateServiceImpl implements TimeMachineUpdateService {
    @Resource
    private TimeMachineMapper timeMachineMapper;

    @Override
    public String categoryUpdate(TimeCategoryDTO categoryDTO) {
        return "";
    }

    @Override
    public String cardUpdate(TimeCardDTO cardDTO) {
        if (cardDTO == null) throw new ParamIllegalException("入参不能为空");
        Integer i = timeMachineMapper.updateCard(cardDTO);
        if (i > 0) {
            return "时光卡片更新成功";
        }
        return "时光卡片更新失败";
    }
}
