package diary.file.service.filetransactionalservice;

import diary.file.entity.po.ThumbnailPO;

/** 缩略图跨表数据库事务；方法内禁止执行OSS或RocketMQ网络调用。 */
public interface ThumbnailTransactionalService {
    void confirmReady(Long imageId, ThumbnailPO thumbnail, int sourceWidth, int sourceHeight);

    void recordPermanentFailure(Long imageId, ThumbnailPO failedThumbnail);

    /** 兼容历史误发事件：无需缩略图的UPLOADED图片直接收敛到READY。 */
    void confirmReadyWithoutThumbnail(Long imageId);
}
