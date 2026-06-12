package diary.diarytimemachine.mapper;

import diary.common.entity.timemachine.po.TimeCardPO;
import diary.common.entity.timemachine.po.TimeCategoryPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TimeMachineMapper {
    Integer categoryAdd(TimeCategoryPO timeCategoryPO);

    Integer cardAdd(TimeCardPO timeCardPO);

    Integer selectSameCategory(String categoryName);

    TimeCategoryPO selectLastCategoryNum();

    TimeCategoryPO selectLastCategorySort();

    Integer selectSameCard(Long categoryId, String cardTitle);
}
