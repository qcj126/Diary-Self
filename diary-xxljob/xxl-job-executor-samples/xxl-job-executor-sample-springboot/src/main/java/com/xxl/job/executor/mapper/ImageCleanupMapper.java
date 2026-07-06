package com.xxl.job.executor.mapper;

import com.xxl.job.executor.entity.ImageCleanupRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ImageCleanupMapper {

    List<ImageCleanupRecord> selectUnreferencedImages();

    int deleteImageById(Long id);
}
