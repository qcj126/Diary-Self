package diary.recipe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"diary.common", "diary.config", "diary.recipe", "diary.utils"})
public class DiaryRecipeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiaryRecipeApplication.class, args);
    }

}
