package diary.recipe.mapper;

import diary.common.entity.recipe.po.RecipeStepPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecipeStepMapper {
    /**
     * 批量插入步骤
     */
    void batchInsert(@Param("list") List<RecipeStepPO> list);

    /**
     * 根据食谱ID查询步骤列表
     */
    List<RecipeStepPO> selectByRecipeId(@Param("recipeId") Long recipeId);

    /**
     * 根据食谱ID列表批量查询
     */
    List<RecipeStepPO> selectByRecipeIds(@Param("recipeIds") List<Long> recipeIds);

    /**
     * 根据食谱ID逻辑删除所有步骤
     */
    void deleteByRecipeId(@Param("recipeId") Long recipeId);

    /**
     * 根据ID列表逻辑删除
     */
    int deleteByIds(@Param("ids") List<Long> ids);

    void batchUpdate(@Param("list") List<RecipeStepPO> steps);
}
