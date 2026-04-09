package diary.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class DiaryDaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiaryDaoApplication.class, args);
        log.info("Diary DAO Service 应用已经启动成功");
    }
}
