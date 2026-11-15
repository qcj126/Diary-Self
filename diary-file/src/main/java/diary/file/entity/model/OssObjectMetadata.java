package diary.file.entity.model;

public record OssObjectMetadata(
        long contentLength,
        String contentType,
        String etag
) {
}