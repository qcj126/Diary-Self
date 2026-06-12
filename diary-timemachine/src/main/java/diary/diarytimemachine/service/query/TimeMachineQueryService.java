package diary.diarytimemachine.service.query;

import diary.common.entity.timemachine.dto.TimeCardDTO;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;
import diary.common.entity.timemachine.vo.TimeCardVO;
import diary.common.entity.timemachine.vo.TimeCategoryVO;

import java.util.List;

public interface TimeMachineQueryService {
    List<TimeCategoryVO> categoryQuery(TimeCategoryDTO categoryDTO);
    List<TimeCardVO> cardQuery(TimeCardDTO cardDTO);
}
