package diary.file.entity.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImageUploadInitVO {
    /** 图片业务记录 ID */
    private Long imageId;

    /**
     * 本系统的上传会话 ID。
     * 秒传成功时可以为 null。
     */
    private String uploadId;

    /** 是否命中 READY 图片并完成秒传 */
    private boolean instantUpload;

    /** 服务端确认的分片大小 */
    private Integer chunkSize;

    /** 服务端确认的分片总数 */
    private Integer totalChunks;

    /** 断点续传时，已经上传成功的分片编号，编号从 0 开始 */
    private List<Integer> uploadedChunkIndexes;
}
