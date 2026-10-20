package diary.diarytimemachine.service.add;

import diary.diarytimemachine.entity.dto.TimeCardDTO;
import diary.diarytimemachine.entity.dto.TimeCategoryDTO;

public interface TimeMachineAddService {
    Integer categoryAdd(TimeCategoryDTO categoryDTO);
    Integer cardAdd(TimeCardDTO cardDTO);
}
