package diary.file.mapper;

import diary.file.entity.po.ImageUploadSessionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ImageUploadSessionMapper {
    int insert(ImageUploadSessionPO session);

    ImageUploadSessionPO selectByUploadIdAndUserId(
            @Param("uploadId") String uploadId,
            @Param("userId") Long userId
    );

    ImageUploadSessionPO selectByImageIdAndUserId(
            @Param("imageId") Long imageId,
            @Param("userId") Long userId
    );

    int updateStatusIfVersion(
            @Param("uploadId") String uploadId,
            @Param("userId") Long userId,
            @Param("expectedStatus") int expectedStatus,
            @Param("targetStatus") int targetStatus,
            @Param("expectedVersion") int expectedVersion
    );

    int deleteByUploadIdAndUserId(
            @Param("uploadId") String uploadId,
            @Param("userId") Long userId
    );
}
