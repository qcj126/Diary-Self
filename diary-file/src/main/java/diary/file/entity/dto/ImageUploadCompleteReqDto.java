package diary.file.entity.dto;

import lombok.Data;

@Data
public class ImageUploadCompleteReqDto {

    /** 初始化接口返回的图片 ID，仅用于与上传会话进行一致性校验 */
    private Long imageId;

    /** 客户端计算的完整文件 SHA-256，仅用于与上传会话进行一致性校验 */
    private String sha256;

    /** 客户端计算的分片总数，仅用于与上传会话进行一致性校验 */
    private Integer totalChunks;
}