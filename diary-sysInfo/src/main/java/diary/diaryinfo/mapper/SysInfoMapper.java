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

    @Select("""
            SELECT MIN(id) AS id,
                   category,
                   category_name AS categoryName
            FROM ingredient
            GROUP BY category, category_name
            ORDER BY MIN(id)
            """)
    List<IngredientPo> selectIngredientCategories();

    @Select("""
            SELECT a.id as id,
                   a.name as name,
                   a.category as category,
                   a.category_name AS categoryName,
                   a.is_main AS isMain,
                   a.icon_id AS iconId,
                   b.icon_name AS iconName,
                   b.icon_path AS iconPath,
                   a.user_id AS userId,
                   a.create_time AS createTime,
                   a.update_time AS updateTime
            FROM ingredient a left join icon b on a.icon_id = b.id
            WHERE a.category = #{condition.category}
              and a.is_main = #{condition.isMain}
            ORDER BY id
            """)
    List<IngredientIconDto> selectIngredientsByCategory(@Param("condition") IngredientPo condition);

    @Select("""
            SELECT id,
                   name,
                   description,
                   user_id AS userId,
                   sort_order AS sortOrder,
                   create_time AS createTime,
                   update_time AS updateTime
            FROM cook_way
            ORDER BY sort_order, id
            """)
    List<CookWayPo> selectAllCookWays();
}
