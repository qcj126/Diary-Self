package diary.file.service.filetransactionalservice;

public interface ImageDeleteTransactionalService {
    void deleteAndRecordCleanupEvent(Long imageId, Long userId);
}
