package diary.diaryai.mapper;

import diary.diaryai.entity.ai.po.AiNutrientPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiNutrientQueryMapper {
    List<AiNutrientPO> selectLatestDietNutrients(
            @Param("userId") Long userId,
            @Param("dietIds") List<Long> dietIds
    );
}
