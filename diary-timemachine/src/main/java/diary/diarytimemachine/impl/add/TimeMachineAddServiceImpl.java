package diary.diarytimemachine.impl.add;

import diary.common.convert.timemachine.DtoConvertToPo;
import diary.common.entity.timemachine.dto.TimeCardDTO;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;
import diary.common.entity.timemachine.po.TimeCardPO;
import diary.common.entity.timemachine.po.TimeCategoryPO;
import diary.common.exception.ParamIllegalException;
import diary.common.exception.SameDataException;
import diary.diarytimemachine.mapper.TimeMachineMapper;
import diary.diarytimemachine.service.add.TimeMachineAddService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TimeMachineAddServiceImpl implements TimeMachineAddService {
    @Resource
    private TimeMachineMapper timeMachineMapper;

    @Override
    public Integer categoryAdd(TimeCategoryDTO categoryDTO) {
        if (categoryDTO == null || categoryDTO.getCategoryName() == null || categoryDTO.getUserId() == null) {
            throw new ParamIllegalException("分类名称或用户ID为空");
        }

        Integer count = timeMachineMapper.selectSameCategory(categoryDTO.getCategoryName());
        if (count > 0) {
            throw new SameDataException("分类已存在");
        }

        TimeCategoryPO timeCategoryPO = DtoConvertToPo.convertToPO(categoryDTO);
        timeCategoryPO.setId(MyUtils.getPrimaryKey());
        Integer lastCategoryNum = timeMachineMapper.selectLastCategoryNum();
        timeCategoryPO.setCategoryNum(lastCategoryNum == null ? 1000 : lastCategoryNum + 1);

        return timeMachineMapper.categoryAdd(timeCategoryPO);
    }

    @Override
    public Integer cardAdd(TimeCardDTO cardDTO) {
        if (cardDTO == null || cardDTO.getCardName() == null || cardDTO.getUserId() == null || cardDTO.getDescription() == null
            || cardDTO.getCardTitle() == null || cardDTO.getCategoryId() == null || cardDTO.getCategoryNum() == null) {
            throw new ParamIllegalException("卡片名称或用户ID或卡片标题或卡片描述或分类ID或分类编号或卡片描述为空");
        }

        TimeCardPO timeCardPO = DtoConvertToPo.convertToPO(cardDTO);
        timeCardPO.setId(MyUtils.getPrimaryKey());

        timeMachineMapper.cardAdd();
        return 0;
    }
}
