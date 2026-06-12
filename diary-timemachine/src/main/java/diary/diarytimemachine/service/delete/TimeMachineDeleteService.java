package diary.diarytimemachine.service.delete;

import diary.common.entity.timemachine.dto.TimeCardDTO;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;

import java.util.List;

public interface TimeMachineDeleteService {
    String categoryDelete(TimeCategoryDTO categoryDTO);
    String cardDelete(TimeCardDTO cardDTO);
}
