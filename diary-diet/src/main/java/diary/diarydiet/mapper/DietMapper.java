package diary.diarydiet.mapper;

import diary.common.entity.diet.po.DietRecordPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DietMapper {
    
    /**
     * 插入饮食记录
     */
    int insert(DietRecordPO dietRecordPO);
    
    /**
     * 根据ID查询饮食记录
     */
    DietRecordPO selectById(@Param("id") Long id);
    
    /**
     * 根据ID更新饮食记录
     */
    int updateById(DietRecordPO dietRecordPO);
    
    /**
     * 根据用户ID查询所有未删除的饮食记录
     */
    List<DietRecordPO> selectByUserId(@Param("userId") Long userId);
}
