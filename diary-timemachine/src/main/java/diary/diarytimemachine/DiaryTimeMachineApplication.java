package diary.diarytimemachine;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"diary.diarytimemachine", "diary.config.mpconfig"})
public class DiaryTimeMachineApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiaryTimeMachineApplication.class, args);
    }

}
