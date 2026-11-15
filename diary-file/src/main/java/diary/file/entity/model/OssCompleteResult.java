package diary.file.entity.model;

import diary.file.enums.OssCompleteState;

public record OssCompleteResult(
        OssCompleteState state,
        String etag,
        long contentLength
) {
}
