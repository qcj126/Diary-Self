package diary.file;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = {"diary.common", "diary.config", "diary.file", "diary.utils"})
public class DiaryFileApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiaryFileApplication.class, args);
        log.info("Diary File Service 应用已经启动成功");
    }
}
