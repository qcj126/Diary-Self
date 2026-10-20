package diary.diarytimemachine.service.update;

import diary.diarytimemachine.entity.dto.TimeCardDTO;
import diary.diarytimemachine.entity.dto.TimeCategoryDTO;

public interface TimeMachineUpdateService {
    String categoryUpdate(TimeCategoryDTO categoryDTO);
    String cardUpdate(TimeCardDTO cardDTO);
}
