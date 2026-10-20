package diary.diaryinfo.mapper;

import diary.diaryinfo.entity.dto.IngredientIconDto;
import diary.diaryinfo.entity.po.CookWayPo;
import diary.diaryinfo.entity.po.IngredientPo;
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
