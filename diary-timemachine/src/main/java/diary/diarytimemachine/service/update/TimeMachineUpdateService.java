package diary.diarytimemachine.service.update;

import diary.common.entity.timemachine.dto.TimeCardDTO;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;

import java.util.List;

public interface TimeMachineUpdateService {
    String categoryUpdate(TimeCategoryDTO categoryDTO);
    String cardUpdate(TimeCardDTO cardDTO);
}
