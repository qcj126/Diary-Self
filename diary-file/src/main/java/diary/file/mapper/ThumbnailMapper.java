package diary.file.mapper;

import diary.file.entity.po.ThumbnailPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ThumbnailMapper {
    int insert(ThumbnailPO thumbnail);

    ThumbnailPO selectByImageIdAndUserId(
            @Param("imageId") Long imageId,
            @Param("userId") Long userId
    );

    List<ThumbnailPO> selectReadyByImageIdsAndUserId(
            @Param("imageIds") List<Long> imageIds,
            @Param("userId") Long userId,
            @Param("imageTypes") List<Integer> imageTypes,
            @Param("imageStatus") int imageStatus,
            @Param("thumbnailStatus") int thumbnailStatus
    );

    ThumbnailPO selectAnyByImageIdAndUserId(
            @Param("imageId") Long imageId,
            @Param("userId") Long userId
    );

    int softDeleteByImageIdAndUserId(
            @Param("imageId") Long imageId,
            @Param("userId") Long userId
    );

    int updateIfStatus(
            @Param("thumbnail") ThumbnailPO thumbnail,
            @Param("expectedStatus") int expectedStatus
    );
}
