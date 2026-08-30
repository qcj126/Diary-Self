package diary.diarylove;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"diary.common", "diary.config", "diary.diarylove", "diary.utils"})
public class DiaryLoveApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiaryLoveApplication.class, args);
    }
}
