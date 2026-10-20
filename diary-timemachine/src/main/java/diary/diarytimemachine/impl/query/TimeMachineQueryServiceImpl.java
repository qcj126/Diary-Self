package diary.diarytimemachine.impl.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import diary.diarytimemachine.entity.convert.DtoToVo;
import diary.diarytimemachine.entity.dto.TimeCardDTO;
import diary.diarytimemachine.entity.dto.TimeCategoryIconDTO;
import diary.diarytimemachine.entity.vo.TimeCardVO;
import diary.diarytimemachine.entity.vo.TimeCategoryVO;
import diary.diarytimemachine.mapper.TimeMachineMapper;
import diary.diarytimemachine.service.query.TimeMachineQueryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TimeMachineQueryServiceImpl implements TimeMachineQueryService {

    @Resource
    private TimeMachineMapper timeMachineMapper;

    @Override
    public List<TimeCategoryVO> categoryQuery() {
        List<TimeCategoryIconDTO> timeCategoryIconDTOS = timeMachineMapper.selectCategory();
        List<TimeCategoryVO> timeCategoryVOS = new ArrayList<>();
        for (TimeCategoryIconDTO timeCategoryIconDTO : timeCategoryIconDTOS) {
            timeCategoryVOS.add(DtoToVo.convertToVo(timeCategoryIconDTO));
        }
        return timeCategoryVOS;
    }

    @Override
    public IPage<TimeCardVO> cardQuery(TimeCardDTO cardDTO) {
        Integer pageIndex = cardDTO.getPageNum();
        Integer pageSize = cardDTO.getPageSize();
        Page<TimeCardVO> page = new Page<>((pageIndex == null ? 0 : pageIndex), (pageSize == null ? 30 : pageSize));
        Long categoryId = cardDTO.getCategoryId(); // 有值查分类、无值查全部
        String exactDate = cardDTO.getExactDate();
        return timeMachineMapper.selectCardPage(page, categoryId, exactDate);
    }
}
