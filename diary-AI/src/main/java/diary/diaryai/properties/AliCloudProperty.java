package diary.diaryai.properties;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ali-cloud.api")
@Data
public class AliCloudProperty{
    private String apiKey;
    private String url;
    private String deepSeekModel;
    private String qwenPlusModel;
    private String qwenMaxModel;
    private Double temperature;
}
