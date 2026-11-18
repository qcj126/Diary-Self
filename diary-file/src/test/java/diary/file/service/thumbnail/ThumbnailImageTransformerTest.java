package diary.file.service.thumbnail;

import diary.file.entity.model.GeneratedThumbnail;
import diary.file.exception.PermanentThumbnailProcessingException;
import diary.file.exception.RetryableThumbnailProcessingException;
import diary.file.property.ThumbnailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThumbnailImageTransformerTest {
    private ThumbnailProperties properties;
    private ThumbnailImageTransformer transformer;

    @BeforeEach
    void setUp() {
        properties = new ThumbnailProperties();
        properties.setMaxWidth(480);
        properties.setMaxHeight(480);
        properties.setJpegQuality(0.85d);
        properties.setMaxPixels(40_000_000);
        transformer = new ThumbnailImageTransformer(properties);
    }

    @Test
    void scalesPngProportionallyAndPreservesAlphaFormat() throws IOException {
        byte[] source = imageBytes("png", 800, 400, true);

        GeneratedThumbnail result = transformer.transform(new ByteArrayInputStream(source), "image/png");

        assertEquals(480, result.width());
        assertEquals(240, result.height());
        assertEquals(800, result.sourceWidth());
        assertEquals(400, result.sourceHeight());
        assertEquals("image/png", result.contentType());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
        assertTrue(decoded.getColorModel().hasAlpha());
    }

    @Test
    void doesNotUpscaleSmallJpeg() throws IOException {
        byte[] source = imageBytes("jpg", 120, 80, false);

        GeneratedThumbnail result = transformer.transform(new ByteArrayInputStream(source), "image/jpeg");

        assertEquals(120, result.width());
        assertEquals(80, result.height());
        assertEquals("jpg", result.extension());
    }

    @Test
    void rejectsMimeThatDoesNotMatchDecodedFormat() throws IOException {
        byte[] source = imageBytes("png", 100, 100, true);

        assertThrows(PermanentThumbnailProcessingException.class,
                () -> transformer.transform(new ByteArrayInputStream(source), "image/jpeg"));
    }

    @Test
    void rejectsImageAboveConfiguredPixelLimitBeforeFullDecode() throws IOException {
        properties.setMaxPixels(9_999);
        byte[] source = imageBytes("png", 100, 100, false);

        assertThrows(PermanentThumbnailProcessingException.class,
                () -> transformer.transform(new ByteArrayInputStream(source), "image/png"));
    }

    @Test
    void treatsSocketReadFailureAsRetryable() {
        InputStream failingNetworkStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new SocketException("connection reset");
            }
        };

        assertThrows(RetryableThumbnailProcessingException.class,
                () -> transformer.transform(failingNetworkStream, "image/jpeg"));
    }

    private byte[] imageBytes(String format, int width, int height, boolean alpha) throws IOException {
        BufferedImage image = new BufferedImage(
                width,
                height,
                alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(alpha ? new Color(255, 0, 0, 120) : Color.RED);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, output);
            return output.toByteArray();
        } finally {
            image.flush();
        }
    }
}
