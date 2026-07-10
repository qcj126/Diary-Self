package diary.common.entity.xxlJob;

import lombok.Data;

@Data
public class ImageCleanUpResultDTO {
    private final int scannedCount;
    private final int deletedCount;
    private final int failedCount;

    public ImageCleanUpResultDTO(int scannedCount, int deletedCount, int failedCount) {
        this.scannedCount = scannedCount;
        this.deletedCount = deletedCount;
        this.failedCount = failedCount;
    }
}
