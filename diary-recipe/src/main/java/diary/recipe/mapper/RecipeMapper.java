package diary.recipe.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import diary.common.entity.recipe.dto.req.RecipePageReqDto;
import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.entity.recipe.po.RecipePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecipeMapper {
    /**
     * 插入食谱
     */
    void insert(RecipePO recipe);

    /**
     * 根据ID查询
     */
    RecipePO selectById(@Param("recipeId") Long recipeId);

    /**
     * 逻辑删除
     */
    int updateStatus(@Param("recipeId") Long recipeId,
                     @Param("status") Integer status,
                     @Param("operatorId") Long operatorId);

    /**
     * 分页查询用户创建的食谱
     */
    List<RecipePO> selectByAuthor(@Param("authorId") Long authorId,
                                  @Param("offset") Integer offset,
                                  @Param("limit") Integer limit);

    /**
     * 条件搜索食谱
     */
    List<RecipePO> search(@Param("keyword") String keyword,
                          @Param("category") Integer category,
                          @Param("mealType") Integer mealType,
                          @Param("difficulty") Integer difficulty,
                          @Param("status") Integer status,
                          @Param("offset") Integer offset,
                          @Param("limit") Integer limit);

    /**
     * 条件搜索总数
     */
    int countSearch(@Param("keyword") String keyword,
                    @Param("category") Integer category,
                    @Param("mealType") Integer mealType,
                    @Param("difficulty") Integer difficulty,
                    @Param("status") Integer status);

    /**
     * 批量更新
     */
    int batchUpdate(@Param("list") List<RecipePO> list);

    /**
     * 查询指定ID列表的食谱
     */
    List<RecipePO> selectByIds(@Param("ids") List<Long> ids);

    RecipePO selectByAuthorTitle(@Param("authorId") Long authorId,
                                 @Param("title") String title,
                                 @Param("mealType") Integer mealType);

    IPage<RecipePO> qryPage(IPage<RecipePO> page, @Param("req") RecipePageReqDto recipePageReqDto);

    void updateById(@Param("req") RecipeReqDto recipeReqDto, @Param("recipeId") Long recipeId);
}
