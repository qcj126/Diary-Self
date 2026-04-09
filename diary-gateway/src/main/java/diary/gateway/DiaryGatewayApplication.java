package diary.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class DiaryGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiaryGatewayApplication.class, args);
        log.info("Diary Gateway Service 应用已经启动成功");
    }
}
