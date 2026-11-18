package diary.file.service.thumbnail;

import diary.file.mq.event.ImageUploadedEvent;

/** 处理原图上传完成事件并可靠生成缩略图。 */
public interface ImageThumbnailProcessService {
    void process(ImageUploadedEvent event);
}
