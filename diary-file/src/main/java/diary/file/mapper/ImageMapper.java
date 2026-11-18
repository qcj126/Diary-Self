package diary.file.mapper;

import diary.file.entity.po.ImagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImageMapper {
    int singleAddImageToDb(
            ImagePO imagePO
    );

    ImagePO selectForUploadByIdAndUserId(
            @Param("imageId") Long imageId,
            @Param("userId") Long userId
    );

    ImagePO selectForThumbnailById(
            @Param("imageId") Long imageId
    );

    ImagePO selectAnyByIdAndUserId(
            @Param("imageId") Long imageId,
            @Param("userId") Long userId
    );

    ImagePO selectAnyByIdAndUserIdForUpdate(
            @Param("imageId") Long imageId,
            @Param("userId") Long userId
    );

    int updateDimensionsIfMissing(
            @Param("imageId") Long imageId,
            @Param("width") int width,
            @Param("height") int height
    );

    ImagePO selectReadyByOriginalNameAndUserId(
            @Param("originalName") String originalName,
            @Param("userId") Long userId
    );

    List<ImagePO> selectLatestTwoImagesForCarousel ();

    int softDeleteOwnedImage (
            @Param("imageId") Long imageId,
            @Param("userId") Long userId
    );

    ImagePO selectImageStatusById (
            @Param("imageId") Long imageId
    );

    List<ImagePO> selectImageStatusesByIdsAndUserId (
            @Param("imageIds") List<Long> imageIds,
            @Param("userId") Long userId
    );

    int updateStatusIfCurrent (
            @Param("imageId") Long imageId,
            @Param("expectedStatus") int expectedStatus,
            @Param("targetStatus") int targetStatus
    );

    int softDeleteAbandonedUpload(
            @Param("imageId") Long imageId,
            @Param("userId") Long userId
    );

    ImagePO selectReadyByUserIdAndTypeAndSha256(
            @Param("userId") Long userId,
            @Param("type") Integer type,
            @Param("sha256") String sha256
    );

    ImagePO selectByUserIdAndTypeAndSha256(
            @Param("userId") Long userId,
            @Param("type") Integer type,
            @Param("sha256") String sha256
    );
}
