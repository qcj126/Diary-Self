package diary.dao.mapper.recipe;

import diary.common.entity.recipe.po.RecipePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecipeMapper {
    /**
     * 插入食谱
     */
    int insert(RecipePO recipe);

    /**
     * 根据ID查询
     */
    RecipePO selectById(@Param("recipeId") Long recipeId);

    /**
     * 根据ID更新（动态更新非空字段）
     */
    int updateById(RecipePO recipe);

    /**
     * 逻辑删除（修改status）
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
     * 分页查询情侣空间食谱
     */
    List<RecipePO> selectByCouple(@Param("coupleId") Long coupleId,
                                  @Param("status") Integer status,
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
     * 更新浏览量 +1
     */
    int incrementViewCount(@Param("recipeId") Long recipeId);

    /**
     * 批量更新点赞数（异步汇总用）
     */
    int batchUpdateLikeCount(@Param("list") List<RecipePO> list);

    /**
     * 查询指定ID列表的食谱
     */
    List<RecipePO> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 根据纪念日查询食谱
     */
    List<RecipePO> selectByAnniversary(@Param("coupleId") Long coupleId,
                                       @Param("date") String date);

    Integer selectByAuthorTitle(Long authorId, String title);
}
