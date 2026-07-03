package diary.diaryai.properties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "deepseek.api")
@Getter
public class DeepSeekProperty{
    private String apiKey;
    private String url;
    private String model;
    private Double temperature;
}
