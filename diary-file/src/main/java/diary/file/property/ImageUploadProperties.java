package diary.file.property;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "diary.file.image-upload")
public class ImageUploadProperties {
    @NotNull
    @Min(1)
    private Integer chunkSize;

    @NotNull
    @Min(1)
    private Integer maxImageSize;

    @NotNull
    @Min(1)
    private Integer sessionTtlHours;
}
