package diary.file.impl.deleteserviceImpl;

import diary.file.service.deleteservice.DeleteService;
import diary.file.service.filetransactionalservice.ImageDeleteTransactionalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 图片删除入口。OSS对象由可靠IMAGE_DELETED事件异步清理。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteServiceImpl implements DeleteService {
    private final ImageDeleteTransactionalService transactionalService;

    @Override
    public String deleteImage(Long id, Long userId) {
        transactionalService.deleteAndRecordCleanupEvent(id, userId);
        log.info("图片逻辑删除及清理事件提交完成, imageId={}, userId={}", id, userId);
        return "删除成功";
    }
}
