package diary.config;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@MapperScan("diary.dao.mapper")
public class DiaryConfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiaryConfigApplication.class, args);
        log.info("Diary Config Service 应用已经启动成功");
    }
}
