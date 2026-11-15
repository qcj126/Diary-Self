package diary.file.mapper;

import diary.file.entity.po.ThumbnailPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ThumbnailMapper {
    int insert(ThumbnailPO thumbnail);

    ThumbnailPO selectByImageIdAndUserId(
            @Param("imageId") Long imageId,
            @Param("userId") Long userId
    );
}
