package diary.diarytimemachine.impl.delete;

import diary.diarytimemachine.entity.dto.TimeCardDTO;
import diary.diarytimemachine.entity.dto.TimeCategoryDTO;
import diary.diarytimemachine.mapper.TimeMachineMapper;
import diary.diarytimemachine.service.delete.TimeMachineDeleteService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TimeMachineDeleteServiceImpl implements TimeMachineDeleteService {
    @Resource
    private TimeMachineMapper timeMachineMapper;

    @Override
    public String categoryDelete(TimeCategoryDTO categoryDTO) {
        Integer i = timeMachineMapper.logicallyDeleteCategory(categoryDTO);
        if (i > 0) {
            return "时光分类删除成功";
        }
        return "时光分类删除成功";
    }

    @Override
    public String cardDelete(TimeCardDTO cardDTO) {
        Integer i = timeMachineMapper.deleteCard(cardDTO);
        if (i > 0) {
            return "时光卡片删除成功";
        }
        return "时光卡片删除失败";
    }
}
