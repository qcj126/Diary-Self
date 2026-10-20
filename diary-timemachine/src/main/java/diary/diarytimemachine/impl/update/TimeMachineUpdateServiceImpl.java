package diary.diarytimemachine.impl.update;

import diary.diarytimemachine.entity.dto.TimeCardDTO;
import diary.diarytimemachine.entity.dto.TimeCategoryDTO;
import diary.common.exception.ParamIllegalException;
import diary.diarytimemachine.mapper.TimeMachineMapper;
import diary.diarytimemachine.service.update.TimeMachineUpdateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
        cardDTO.setUpdateTime(LocalDateTime.now());
        Integer i = timeMachineMapper.updateCard(cardDTO);
        if (i > 0) {
            return "时光卡片更新成功";
        }
        return "时光卡片更新失败";
    }
}
