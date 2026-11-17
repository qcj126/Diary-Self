package diary.file.mapper;

import diary.file.entity.po.ImagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImageMapper {
    Integer batchAddImageToDb (
            @Param("imageList") List<ImagePO> imageList
    );

    int singleAddImageToDb(
            ImagePO imagePO
    );

    Long selectImageByTypeAndName (
            @Param("type") Integer type,
            @Param("originalFilename") String originalFilename
    );

    Integer updateImageStatusById (
            @Param("id") Long id,
            @Param("objectKey") String objectKey,
            @Param("status") int status
    );

    ImagePO selectImageById (
            @Param("id") Long id
    );

    ImagePO selectForUploadByIdAndUserId(
            @Param("imageId") Long imageId,
            @Param("userId") Long userId
    );

    ImagePO selectImageByOriginalName (
            @Param("originalName") String originalName
    );

    List<ImagePO> selectImagesForUploadByIds (
            @Param("imageIds") List<Long> imageIds
    );

    List<ImagePO> selectLatestTwoImagesForCarousel ();

    Integer deleteImageById (
            @Param("id") Long id
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
