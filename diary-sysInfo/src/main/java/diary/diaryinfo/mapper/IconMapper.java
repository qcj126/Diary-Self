package diary.diaryinfo.mapper;

import diary.common.entity.file.dto.IconDTO;
import diary.common.entity.file.po.IconPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IconMapper {
    Integer insertIcon(IconPO iconPO);

    List<IconPO> selectIcons(IconDTO iconDTO);

    IconPO selectIconById(@Param("id") Long id);

    Integer updateIcon(IconPO iconPO);

    Integer deleteIcon(@Param("id") Long id, @Param("userId") Long userId);
}
