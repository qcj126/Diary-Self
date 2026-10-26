package diary.diarytimemachine.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import diary.diarytimemachine.entity.dto.TimeCardDTO;
import diary.diarytimemachine.entity.dto.TimeCategoryDTO;
import diary.diarytimemachine.entity.dto.TimeCategoryIconDTO;
import diary.diarytimemachine.entity.po.TimeCardPO;
import diary.diarytimemachine.entity.po.TimeCategoryPO;
import diary.diarytimemachine.entity.vo.TimeCardVO;
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

    IPage<TimeCardVO> selectCardPage(IPage<TimeCardVO> page, @Param("categoryId") Long categoryId,
                                     @Param("exactDate") String exactDate, @Param("userId") Long userId);

    Integer deleteCard(TimeCardDTO cardDTO);

    Integer updateCard(TimeCardDTO cardDTO);

    Integer logicallyDeleteCategory(TimeCategoryDTO categoryDTO);

    List<TimeCategoryIconDTO> selectCategory(@Param("userId") Long userId);

}
