package diary.file.mapper;

import diary.common.entity.file.dto.IconQueryDTO;
import diary.common.entity.file.po.IconPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IconMapper {
    Integer insertIcon(IconPO iconPO);

    List<IconPO> selectIcons(IconQueryDTO queryDTO);

    IconPO selectIconById(@Param("id") Long id);

    Integer updateIcon(IconPO iconPO);

    Integer deleteIcon(@Param("id") Long id, @Param("userId") Long userId);
}
