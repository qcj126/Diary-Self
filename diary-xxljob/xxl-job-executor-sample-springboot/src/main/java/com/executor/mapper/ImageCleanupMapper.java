package com.executor.mapper;

import diary.common.entity.xxlJob.ImageCleanupRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ImageCleanupMapper {

    List<ImageCleanupRecord> selectUnreferencedImages();

    int deleteImageById(Long id);
}
