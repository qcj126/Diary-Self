package diary.file.service.thumbnail;

import diary.file.entity.model.GeneratedThumbnail;
import diary.file.exception.PermanentThumbnailProcessingException;
import diary.file.exception.RetryableThumbnailProcessingException;
import diary.file.property.ThumbnailProperties;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Iterator;
import java.util.Locale;

/**
 * 使用JDK ImageIO安全读取并编码JPEG/PNG缩略图。
 *
 * <p>使用MemoryCacheImageInputStream，避免ImageIO把网络对象写入节点临时目录；在真正解码像素前
 * 先读取宽高并验证最大像素数。缩略图输出由最大宽高推导硬上限，避免ByteArrayOutputStream无限增长。</p>
 */
@Component
@RequiredArgsConstructor
public class ThumbnailImageTransformer {
    private static final String JPEG_MIME = "image/jpeg";
    private static final String PNG_MIME = "image/png";
    private static final int ENCODE_OVERHEAD_BYTES = 64 * 1024;

    private final ThumbnailProperties properties;

    public GeneratedThumbnail transform(InputStream source, String expectedMimeType) {
        MyUtils.check()
                .notNull(source, "source")
                .notEmpty(expectedMimeType, "expectedMimeType");
        String normalizedMimeType = normalizeMimeType(expectedMimeType);

        ImageReader reader = null;
        BufferedImage original = null;
        BufferedImage scaled = null;
        FailureTrackingInputStream trackedSource = new FailureTrackingInputStream(source);
        try (ImageInputStream imageInput = new MemoryCacheImageInputStream(trackedSource)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                if (trackedSource.getFailure() != null) {
                    throw new RetryableThumbnailProcessingException(
                            "读取OSS原图头时发生I/O异常", trackedSource.getFailure()
                    );
                }
                throw new PermanentThumbnailProcessingException("原图不是可解码的JPEG或PNG");
            }

            reader = readers.next();
            reader.setInput(imageInput, false, true);
            String actualMimeType = mimeTypeForFormat(reader.getFormatName());
            if (!normalizedMimeType.equals(actualMimeType)) {
                throw new PermanentThumbnailProcessingException(
                        "原图实际格式与数据库MIME不一致, expected=" + normalizedMimeType
                                + ", actual=" + actualMimeType
                );
            }

            int sourceWidth = reader.getWidth(0);
            int sourceHeight = reader.getHeight(0);
            validateDimensions(sourceWidth, sourceHeight);

            ImageReadParam readParam = reader.getDefaultReadParam();
            original = reader.read(0, readParam);
            if (original == null) {
                throw new PermanentThumbnailProcessingException("ImageIO没有返回解码后的原图");
            }

            int targetWidth = targetSize(sourceWidth, sourceHeight)[0];
            int targetHeight = targetSize(sourceWidth, sourceHeight)[1];
            scaled = scale(original, targetWidth, targetHeight, actualMimeType);
            byte[] encoded = encode(scaled, actualMimeType);

            return new GeneratedThumbnail(
                    encoded,
                    actualMimeType,
                    PNG_MIME.equals(actualMimeType) ? "png" : "jpg",
                    targetWidth,
                    targetHeight,
                    sourceWidth,
                    sourceHeight
            );
        } catch (PermanentThumbnailProcessingException exception) {
            throw exception;
        } catch (IIOException exception) {
            if (trackedSource.getFailure() != null || hasTransientNetworkCause(exception)) {
                throw new RetryableThumbnailProcessingException("读取OSS原图时发生临时网络异常", exception);
            }
            throw new PermanentThumbnailProcessingException("原图结构损坏或无法解码", exception);
        } catch (IOException exception) {
            throw new RetryableThumbnailProcessingException("读取或编码原图时发生I/O异常", exception);
        } catch (ArithmeticException exception) {
            throw new PermanentThumbnailProcessingException("图片尺寸计算溢出", exception);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
            if (scaled != null && scaled != original) {
                scaled.flush();
            }
            if (original != null) {
                original.flush();
            }
        }
    }

    private void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new PermanentThumbnailProcessingException("原图宽高必须大于0");
        }
        long pixels = Math.multiplyExact((long) width, (long) height);
        if (pixels > properties.getMaxPixels()) {
            throw new PermanentThumbnailProcessingException(
                    "原图像素数超过限制, actual=" + pixels + ", max=" + properties.getMaxPixels()
            );
        }
    }

    private int[] targetSize(int sourceWidth, int sourceHeight) {
        double widthScale = properties.getMaxWidth() / (double) sourceWidth;
        double heightScale = properties.getMaxHeight() / (double) sourceHeight;
        double scale = Math.min(1.0d, Math.min(widthScale, heightScale));
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        return new int[]{width, height};
    }

    private BufferedImage scale(BufferedImage source, int width, int height, String mimeType) {
        if (source.getWidth() == width && source.getHeight() == height
                && !(JPEG_MIME.equals(mimeType) && source.getColorModel().hasAlpha())) {
            return source;
        }

        int imageType = PNG_MIME.equals(mimeType) && source.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;
        BufferedImage target = new BufferedImage(width, height, imageType);
        Graphics2D graphics = target.createGraphics();
        try {
            if (JPEG_MIME.equals(mimeType)) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
            }
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private byte[] encode(BufferedImage image, String mimeType) throws IOException {
        String format = PNG_MIME.equals(mimeType) ? "png" : "jpeg";
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            throw new PermanentThumbnailProcessingException("JVM没有可用的" + format + "图片编码器");
        }

        long rawSize = Math.multiplyExact(Math.multiplyExact((long) image.getWidth(), image.getHeight()), 4L);
        int maxEncodedBytes = Math.toIntExact(Math.addExact(rawSize, ENCODE_OVERHEAD_BYTES));
        BoundedOutputStream bounded = new BoundedOutputStream(maxEncodedBytes);
        ImageWriter writer = writers.next();
        try (ImageOutputStream output = new MemoryCacheImageOutputStream(bounded)) {
            writer.setOutput(output);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (JPEG_MIME.equals(mimeType) && writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(properties.getJpegQuality().floatValue());
            }
            writer.write(null, new IIOImage(image, null, null), writeParam);
            output.flush();
            return bounded.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private String normalizeMimeType(String mimeType) {
        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(normalized)) {
            return JPEG_MIME;
        }
        if (!JPEG_MIME.equals(normalized) && !PNG_MIME.equals(normalized)) {
            throw new PermanentThumbnailProcessingException("不支持的原图MIME：" + mimeType);
        }
        return normalized;
    }

    private String mimeTypeForFormat(String formatName) {
        String normalized = formatName == null ? "" : formatName.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "jpeg", "jpg" -> JPEG_MIME;
            case "png" -> PNG_MIME;
            default -> throw new PermanentThumbnailProcessingException("不支持的实际图片格式：" + formatName);
        };
    }

    private boolean hasTransientNetworkCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketException || current instanceof InterruptedIOException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /** 记录可能被ImageIO探测逻辑吞掉的底层OSS输入流异常。 */
    private static final class FailureTrackingInputStream extends FilterInputStream {
        private IOException failure;

        private FailureTrackingInputStream(InputStream source) {
            super(source);
        }

        @Override
        public int read() throws IOException {
            try {
                return super.read();
            } catch (IOException exception) {
                failure = exception;
                throw exception;
            }
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            try {
                return super.read(bytes, offset, length);
            } catch (IOException exception) {
                failure = exception;
                throw exception;
            }
        }

        private IOException getFailure() {
            return failure;
        }
    }

    /** 只允许写入由缩略图最大尺寸推导出的有限字节数。 */
    private static final class BoundedOutputStream extends OutputStream {
        private final int maxBytes;
        private final ByteArrayOutputStream delegate;

        private BoundedOutputStream(int maxBytes) {
            this.maxBytes = maxBytes;
            this.delegate = new ByteArrayOutputStream(Math.min(maxBytes, 64 * 1024));
        }

        @Override
        public void write(int value) throws IOException {
            ensureCapacity(1);
            delegate.write(value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            ensureCapacity(length);
            delegate.write(bytes, offset, length);
        }

        private void ensureCapacity(int additionalBytes) throws IOException {
            if ((long) delegate.size() + additionalBytes > maxBytes) {
                throw new IOException("缩略图编码结果超过内存上限：" + maxBytes + "字节");
            }
        }

        private byte[] toByteArray() {
            return delegate.toByteArray();
        }
    }
}
