package diary.diaryai.recovery.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TaskRecoveredEvent extends ApplicationEvent {
    private final Long taskId;
    public TaskRecoveredEvent(Object source, Long taskId) {
        super(source);
        this.taskId = taskId;
    }
}
