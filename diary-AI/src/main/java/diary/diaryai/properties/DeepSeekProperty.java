package diary.diaryai.properties;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "deepseek.api")
@Data
public class DeepSeekProperty{
    private String key;
    private String url;
    private String model;
    private Double temperature;
}
