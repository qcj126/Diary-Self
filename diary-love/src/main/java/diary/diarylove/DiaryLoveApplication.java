package diary.diarylove;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"diary.diarylove", "diary.common.auth"})
public class DiaryLoveApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiaryLoveApplication.class, args);
    }
}
