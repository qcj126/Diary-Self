package diary.diaryai.entity.ai.dto;

public class LoveRecordSentimentSubmitDTO {
    private String clientRequestId;                             // 客户端请求ID    防止用户重复点击导致重复创建任务
    private Integer aiType;                                     // 调用AI的枚举值
    private Long loveRecordId;                                  // 恋爱记录表主键值
}
