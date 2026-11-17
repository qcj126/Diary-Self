package diary.file.entity.dto;

import lombok.Data;

@Data
public class ImageUploadInitReqDto {
    /** 客户端选择文件时的原始文件名 */
    private String originalName;

    /** 文件总大小，单位：字节 */
    private Long fileSize;

    /** 文件 MIME 类型，只允许 image/png 或 image/jpeg */
    private String mimeType;

    /** 整个文件的 SHA-256，小写十六进制字符串 */
    private String sha256;

    /** 客户端使用的分片大小，必须与服务端当前配置一致 */
    private Integer chunkSize;

    /** 客户端计算出的分片总数 */
    private Integer totalChunks;

    /** 图片所属的业务类型编码 */
    private Integer imageTypeCode;
}
