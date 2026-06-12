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
        TimeCategoryPO lastCategoryNum = timeMachineMapper.selectLastCategoryNum();
        timeCategoryPO.setCategoryNum(lastCategoryNum.getCategoryNum() == null ? 1000 : lastCategoryNum.getCategoryNum() + 1);
        TimeCategoryPO lastCategorySort = timeMachineMapper.selectLastCategorySort();
        timeCategoryPO.setSort(lastCategorySort.getSort() == null ? 1 : lastCategorySort.getSort() + 1);
        return timeMachineMapper.categoryAdd(timeCategoryPO);
    }

    @Override
    public Integer cardAdd(TimeCardDTO cardDTO) {
        if (cardDTO == null || cardDTO.getUserId() == null || cardDTO.getCardContent() == null || cardDTO.getCardTitle() == null
                || cardDTO.getCategoryId() == null || cardDTO.getImageId() == null || cardDTO.getRecordTime() == null) {
            throw new ParamIllegalException("卡片名称或用户ID或卡片标题或卡片内容或分类ID或图片ID或记录时间为空");
        }

        Integer count = timeMachineMapper.selectSameCard(cardDTO.getCategoryId(), cardDTO.getCardTitle());
        if (count > 0) {
            throw new SameDataException("此分类下已存在相同标题的卡片");
        }
        TimeCardPO timeCardPO = DtoConvertToPo.convertToPO(cardDTO);
        timeCardPO.setId(MyUtils.getPrimaryKey());
        return timeMachineMapper.cardAdd(timeCardPO);
    }
}
