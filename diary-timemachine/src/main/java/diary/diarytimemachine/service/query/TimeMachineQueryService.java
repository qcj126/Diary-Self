package diary.diarytimemachine.service.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import diary.diarytimemachine.entity.dto.TimeCardDTO;
import diary.diarytimemachine.entity.vo.TimeCardVO;
import diary.diarytimemachine.entity.vo.TimeCategoryVO;

import java.util.List;

public interface TimeMachineQueryService {
    List<TimeCategoryVO> categoryQuery();
    IPage<TimeCardVO> cardQuery(TimeCardDTO cardDTO);
}
