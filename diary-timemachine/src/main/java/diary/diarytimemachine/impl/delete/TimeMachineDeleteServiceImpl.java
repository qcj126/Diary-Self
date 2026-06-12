package diary.diarytimemachine.impl.delete;

import diary.common.convert.timemachine.DtoConvertToPo;
import diary.common.entity.timemachine.dto.TimeCardDTO;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;
import diary.common.entity.timemachine.po.TimeCardPO;
import diary.common.entity.timemachine.po.TimeCategoryPO;
import diary.common.exception.ParamIllegalException;
import diary.common.exception.SameDataException;
import diary.diarytimemachine.mapper.TimeMachineMapper;
import diary.diarytimemachine.service.add.TimeMachineAddService;
import diary.diarytimemachine.service.delete.TimeMachineDeleteService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TimeMachineDeleteServiceImpl implements TimeMachineDeleteService {

    @Override
    public String categoryDelete(TimeCategoryDTO categoryDTO) {
        return "";
    }

    @Override
    public String cardDelete(TimeCardDTO cardDTO) {
        return "";
    }
}
