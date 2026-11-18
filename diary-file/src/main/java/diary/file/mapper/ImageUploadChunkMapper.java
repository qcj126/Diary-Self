package diary.file.mapper;

import diary.file.entity.po.ImageUploadChunkPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImageUploadChunkMapper {
    int insert(ImageUploadChunkPO chunk);

    ImageUploadChunkPO selectByUploadIdAndChunkIndex(
            @Param("uploadId") String uploadId,
            @Param("chunkIndex") Integer chunkIndex
    );

    List<ImageUploadChunkPO> selectByUploadIdOrderByPartNumber(
            @Param("uploadId") String uploadId
    );

    int countByUploadId(@Param("uploadId") String uploadId);

    int deleteByUploadId(@Param("uploadId") String uploadId);

    int deleteOrphans(@Param("limit") int limit);
}
