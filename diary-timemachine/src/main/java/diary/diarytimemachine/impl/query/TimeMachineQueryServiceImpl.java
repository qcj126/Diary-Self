package diary.diarytimemachine.impl.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import diary.common.convert.timemachine.PoConvertToVo;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;
import diary.common.entity.timemachine.po.TimeCategoryPO;
import diary.common.entity.timemachine.vo.TimeCardVO;
import diary.common.entity.timemachine.vo.TimeCategoryVO;
import diary.diarytimemachine.mapper.TimeMachineMapper;
import diary.diarytimemachine.service.query.TimeMachineQueryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Service
public class TimeMachineQueryServiceImpl implements TimeMachineQueryService {

    @Resource
    private TimeMachineMapper timeMachineMapper;

    @Override
    public List<TimeCategoryVO> categoryQuery() {
        List<TimeCategoryPO> timeCategoryPOS = timeMachineMapper.selectCategory();
        List<TimeCategoryVO> timeCategoryVOS = new ArrayList<>();
        for (TimeCategoryPO timeCategoryPO : timeCategoryPOS) {
            timeCategoryVOS.add(PoConvertToVo.convertToTimeCategoryVO(timeCategoryPO));
        }
        return timeCategoryVOS;
    }

    @Override
    public IPage<TimeCardVO> cardQuery(@RequestParam("pageIndex") Integer pageIndex, @RequestParam("pageSize") Integer pageSize) {
        Page<TimeCardVO> page = new Page<>(pageIndex, pageSize);
        return timeMachineMapper.selectCardPage(page);
    }
}
