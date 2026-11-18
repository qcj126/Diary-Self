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

    /** 过期会话清理任务执行间隔。 */
    @NotNull
    @Min(1000)
    private Long cleanupIntervalMs;

    /** 单次清理上限，避免长事务和OSS请求突增。 */
    @NotNull
    @Min(1)
    private Integer cleanupBatchSize;
}
