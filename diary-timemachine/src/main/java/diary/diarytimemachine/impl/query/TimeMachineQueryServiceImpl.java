package diary.diarytimemachine.impl.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;
import diary.common.entity.timemachine.vo.TimeCardVO;
import diary.common.entity.timemachine.vo.TimeCategoryVO;
import diary.diarytimemachine.mapper.TimeMachineMapper;
import diary.diarytimemachine.service.query.TimeMachineQueryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Service
public class TimeMachineQueryServiceImpl implements TimeMachineQueryService {

    @Resource
    private TimeMachineMapper timeMachineMapper;

    @Override
    public List<TimeCategoryVO> categoryQuery(TimeCategoryDTO categoryDTO) {
        return List.of();
    }

    @Override
    public IPage<TimeCardVO> cardQuery(@RequestParam("pageIndex") Integer pageIndex, @RequestParam("pageSize") Integer pageSize) {
        Page<TimeCardVO> page = new Page<>(pageIndex, pageSize);
        return timeMachineMapper.selectCardPage(page);
    }
}
