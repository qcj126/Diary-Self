package diary.recipe.mapper;

import diary.common.entity.recipe.po.RecipeIngredientPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecipeIngredientMapper {
    /**
     * 批量插入食材
     */
    void batchInsert(@Param("list") List<RecipeIngredientPO> list);

    /**
     * 根据食谱ID查询食材列表
     */
    List<RecipeIngredientPO> selectByRecipeId(@Param("recipeId") Long recipeId);

    /**
     * 根据食谱ID列表批量查询
     */
    List<RecipeIngredientPO> selectByRecipeIds(@Param("recipeIds") List<Long> recipeIds);

    /**
     * 根据食谱ID逻辑删除所有食材
     */
    void deleteByRecipeId(@Param("recipeId") Long recipeId);

    /**
     * 根据ID列表逻辑删除
     */
    int deleteByIds(@Param("ids") List<Long> ids);

    void batchUpdate(@Param("list") List<RecipeIngredientPO> ingredients);
}
