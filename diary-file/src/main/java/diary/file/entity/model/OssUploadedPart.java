package diary.file.entity.model;

public record OssUploadedPart(
        /*OSS 分片编号，从1开始。*/
        int partNumber,
        /*OSS 返回的 ETag，Complete 时必须原样传回。*/
        String etag,
        /*本次上传的实际字节数。*/
        long partSize
) {}
