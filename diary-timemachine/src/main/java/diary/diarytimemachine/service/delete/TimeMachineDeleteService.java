package diary.diarytimemachine.service.delete;

import diary.diarytimemachine.entity.dto.TimeCardDTO;
import diary.diarytimemachine.entity.dto.TimeCategoryDTO;

public interface TimeMachineDeleteService {
    String categoryDelete(TimeCategoryDTO categoryDTO);
    String cardDelete(TimeCardDTO cardDTO);
}
