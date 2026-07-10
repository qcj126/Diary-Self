package com.executor.service;

import diary.common.entity.xxlJob.ImageCleanUpResultDTO;

public interface ImageCleanupService {

    ImageCleanUpResultDTO cleanupUnreferencedImages();

}
