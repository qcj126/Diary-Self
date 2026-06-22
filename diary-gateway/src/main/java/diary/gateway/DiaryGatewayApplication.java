package diary.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"diary.gateway", "diary.utils.jwt"})
public class DiaryGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiaryGatewayApplication.class, args);
    }

}
