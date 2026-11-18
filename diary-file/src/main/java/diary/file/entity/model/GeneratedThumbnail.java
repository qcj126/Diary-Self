package diary.file.entity.model;

/**
 * 已完成编码、可直接上传OSS的缩略图。
 *
 * @param bytes       受尺寸上限约束的编码结果
 * @param contentType image/jpeg或image/png
 * @param extension   jpg或png
 * @param width       缩略图宽度
 * @param height      缩略图高度
 * @param sourceWidth 原图宽度
 * @param sourceHeight 原图高度
 */
public record GeneratedThumbnail(
        byte[] bytes,
        String contentType,
        String extension,
        int width,
        int height,
        int sourceWidth,
        int sourceHeight
) {
}
