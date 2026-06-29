package diary.diarygoal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"diary.common", "diary.config", "diary.diarygoal", "diary.utils"})
public class DiaryGoalApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiaryGoalApplication.class, args);
    }

}
