package diary;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class DiaryAIApplication{
    public static void main(String[] args) {
        SpringApplication.run(DiaryAIApplication.class, args);
        log.info("Diary AI Service 应用已经启动成功");
    }
}