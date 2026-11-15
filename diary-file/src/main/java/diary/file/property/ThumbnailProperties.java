package diary.file.property;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "diary.file.thumbnail")
public class ThumbnailProperties {
    @NotNull
    @Min(1)
    private Integer maxWidth;
    @NotNull
    @Min(1)
    private Integer maxHeight;
    @NotNull
    @Min(0)
    @Max(1)
    private Double jpegQuality;
    @NotNull
    @Min(1)
    private Integer maxPixels;
}
