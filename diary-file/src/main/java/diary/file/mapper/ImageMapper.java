package diary.file.mapper;

import diary.common.entity.image.po.ImagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImageMapper {
    Integer batchAddImageToDb(@Param("imageList") List<ImagePO> imageList);

    Integer selectImageByTypeAndName(Integer type, String originalFilename);

    Integer updateImageStatusById(Long id, String ossUrl, int status);

    List<ImagePO> selectImagesByIds(List<Long> imageIds);
}
