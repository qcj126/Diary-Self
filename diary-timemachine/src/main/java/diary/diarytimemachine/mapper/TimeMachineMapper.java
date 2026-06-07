package diary.diarytimemachine.mapper;

import diary.common.entity.timemachine.po.TimeCategoryPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TimeMachineMapper {
    Integer categoryAdd(TimeCategoryPO timeCategoryPO);

    Integer cardAdd();

    Integer selectLastCategoryNum();

    Integer selectSameCategory(String categoryName);
}
