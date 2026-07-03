package diary.diaryai.webclient;

import diary.diaryai.properties.DeepSeekProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient deepSeekWebClient(WebClient.Builder builder, DeepSeekProperty deepSeekProperty) {
        return builder.baseUrl(deepSeekProperty.getUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + deepSeekProperty.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }


}
