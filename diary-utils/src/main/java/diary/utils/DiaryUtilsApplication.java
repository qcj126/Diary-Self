package diary.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class DiaryUtilsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiaryUtilsApplication.class, args);
        log.info("Diary Utils Service 应用已经启动成功");
    }
}
