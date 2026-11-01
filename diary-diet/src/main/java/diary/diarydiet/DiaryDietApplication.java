package diary.diarydiet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"diary.common", "diary.config", "diary.diarydiet", "diary.utils"})
@EnableFeignClients(basePackages = "diary.diarydiet.feignclient")
public class DiaryDietApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiaryDietApplication.class, args);
    }

}
