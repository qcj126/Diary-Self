package diary.recipe.mapper;

import diary.common.entity.recipe.po.RecipeCategoryPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RecipeCategoryMapper {
    List<RecipeCategoryPO> queryRecipeCategory();

    Integer deleteByIds(List<Long> ids);

    Integer selectMaxCategoryNum();

    Integer insert(RecipeCategoryPO recipeCategoryPO);
}
