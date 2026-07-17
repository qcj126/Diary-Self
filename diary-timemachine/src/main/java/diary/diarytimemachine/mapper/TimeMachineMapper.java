package diary.diarytimemachine.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import diary.common.entity.timemachine.dto.TimeCardDTO;
import diary.common.entity.timemachine.po.TimeCardPO;
import diary.common.entity.timemachine.po.TimeCategoryPO;
import diary.common.entity.timemachine.vo.TimeCardVO;
import diary.common.entity.timemachine.vo.TimeCategoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TimeMachineMapper {
    Integer categoryAdd(TimeCategoryPO timeCategoryPO);

    Integer cardAdd(TimeCardPO timeCardPO);

    Integer selectSameCategory(String categoryName);

    TimeCategoryPO selectLastCategoryNum();

    TimeCategoryPO selectLastCategorySort();

    Integer selectSameCard(Long categoryId, String cardTitle);

    IPage<TimeCardVO> selectCardPage(IPage<TimeCardVO> page);

    Integer logicallyDeleteCard(Long id);

    Integer updateCard(TimeCardDTO cardDTO);

    Integer logicallyDeleteCategory(Long id);

    List<TimeCategoryPO> selectCategory();

}
