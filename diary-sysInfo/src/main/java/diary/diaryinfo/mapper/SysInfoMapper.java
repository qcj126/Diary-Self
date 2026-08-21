package diary.diaryinfo.mapper;

import diary.common.entity.sysInfo.dto.IngredientIconDto;
import diary.common.entity.sysInfo.po.CookWayPo;
import diary.common.entity.sysInfo.po.IngredientPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysInfoMapper {

    List<IngredientPo> selectIngredientCategories();

    List<IngredientIconDto> selectIngredientsByCategory(@Param("condition") IngredientPo condition);

    List<CookWayPo> selectAllCookWays();
}
