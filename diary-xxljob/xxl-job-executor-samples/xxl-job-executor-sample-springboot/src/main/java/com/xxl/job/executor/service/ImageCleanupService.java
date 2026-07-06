package com.xxl.job.executor.service;

public interface ImageCleanupService {

    ImageCleanupResult cleanupUnreferencedImages();

    class ImageCleanupResult {
        private final int scannedCount;
        private final int deletedCount;
        private final int failedCount;

        public ImageCleanupResult(int scannedCount, int deletedCount, int failedCount) {
            this.scannedCount = scannedCount;
            this.deletedCount = deletedCount;
            this.failedCount = failedCount;
        }

        public int getScannedCount() {
            return scannedCount;
        }

        public int getDeletedCount() {
            return deletedCount;
        }

        public int getFailedCount() {
            return failedCount;
        }
    }
}
