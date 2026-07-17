package diary.diarytimemachine.service.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;
import diary.common.entity.timemachine.vo.TimeCardVO;
import diary.common.entity.timemachine.vo.TimeCategoryVO;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface TimeMachineQueryService {
    List<TimeCategoryVO> categoryQuery();
    IPage<TimeCardVO> cardQuery(@RequestParam("pageIndex") Integer pageIndex, @RequestParam("pageSize") Integer pageSize);
}
