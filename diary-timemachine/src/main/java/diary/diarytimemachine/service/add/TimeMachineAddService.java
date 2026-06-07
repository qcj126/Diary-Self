package diary.diarytimemachine.service.add;

import diary.common.entity.timemachine.dto.TimeCardDTO;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;

public interface TimeMachineAddService {
    Integer categoryAdd(TimeCategoryDTO categoryDTO);
    Integer cardAdd(TimeCardDTO cardDTO);
}
