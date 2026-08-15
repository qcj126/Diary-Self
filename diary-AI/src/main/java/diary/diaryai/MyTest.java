package diary.diaryai;

import diary.common.enums.aienum.AiTaskErrorCodeEnum;
import diary.common.enums.aienum.AiTaskStatusEnum;
import diary.common.exception.AiSubmitRateLimitException;

public class MyTest {
    public static void main(String[] args) {
        AiTaskStatusEnum status = AiTaskStatusEnum.PENDING;
        System.out.println(status.name());
        System.out.println(status.getDisplayName());
        throw new AiSubmitRateLimitException(AiTaskErrorCodeEnum.PERMANENT_ERROR.name());
    }
}
