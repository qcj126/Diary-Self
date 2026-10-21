package diary.diaryrecipe.mapper;

import diary.diaryrecipe.entity.dto.RecipeCategoryIconDto;
import diary.diaryrecipe.entity.po.RecipeCategoryPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RecipeCategoryMapper {
    List<RecipeCategoryIconDto> queryRecipeCategory();

    Integer deleteByIds(List<Long> ids);

    Integer selectMaxCategoryNum();

    Integer insert(RecipeCategoryPO recipeCategoryPO);

    int updateCategoryById(RecipeCategoryPO recipeCategoryPO);

}
