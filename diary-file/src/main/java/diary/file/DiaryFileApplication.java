package diary.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class DiaryFileApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiaryFileApplication.class, args);
        log.info("Diary File Service 应用已经启动成功");
    }
}
