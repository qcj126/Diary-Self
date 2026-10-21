package diary.diaryrecipe.mapper;

import diary.diaryrecipe.entity.dto.IngredientIconDto;
import diary.diaryrecipe.entity.po.CookWayPo;
import diary.diaryrecipe.entity.po.IngredientPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysInfoMapper {

    List<IngredientPo> selectIngredientCategories(@Param("isMain") Integer isMain);

    List<IngredientIconDto> selectIngredientsByCategory(@Param("condition") IngredientPo condition);

    List<CookWayPo> selectAllCookWays();

    int insertIngredient(IngredientPo ingredientPo);
}
