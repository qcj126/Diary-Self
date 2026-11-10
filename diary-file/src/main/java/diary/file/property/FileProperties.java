package diary.file.property;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "diary.file.part")
public class FileProperties {
    @NotNull
    @Min(1)
    private Long partSize;

    @NotNull
    @Min(1)
    private Long fileThreshold;
}
