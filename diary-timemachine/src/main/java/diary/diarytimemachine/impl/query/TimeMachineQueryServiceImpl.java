package diary.diarytimemachine.impl.query;

import diary.common.entity.timemachine.dto.TimeCardDTO;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;
import diary.common.entity.timemachine.vo.TimeCardVO;
import diary.common.entity.timemachine.vo.TimeCategoryVO;
import diary.diarytimemachine.service.query.TimeMachineQueryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TimeMachineQueryServiceImpl implements TimeMachineQueryService {

    @Override
    public List<TimeCategoryVO> categoryQuery(TimeCategoryDTO categoryDTO) {
        return List.of();
    }

    @Override
    public List<TimeCardVO> cardQuery(TimeCardDTO cardDTO) {
        return List.of();
    }
}
