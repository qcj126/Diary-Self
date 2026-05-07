package diary.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = {"diary.common", "diary.dao", "diary.config", "diary.user", "diary.utils"})
public class DiaryUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiaryUserApplication.class, args);
        log.info("Diary User Service 应用已经启动成功");
    }
}
