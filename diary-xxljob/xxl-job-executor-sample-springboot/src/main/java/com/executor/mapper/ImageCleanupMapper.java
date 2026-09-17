package com.executor.mapper;

import diary.common.entity.xxlJob.ImageCleanupRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImageCleanupMapper {

    List<ImageCleanupRecord> selectUnreferencedImages(@Param("retentionHours") int retentionHours,
                                                      @Param("batchSize") int batchSize);

    List<ImageCleanupRecord> selectImagesByObjectKeys(@Param("objectKeys") List<String> objectKeys);

    int deleteImageByIdIfUnreferenced(@Param("id") Long id,
                                      @Param("retentionHours") int retentionHours);
}
