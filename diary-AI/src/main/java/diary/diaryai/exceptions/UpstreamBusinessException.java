package diary.diaryai.exceptions;

import lombok.Getter;

/**
 * 上游服务确认请求无效或关联业务对象不存在，重试不会改变结果。
 */
@Getter
public class UpstreamBusinessException extends RuntimeException {
    private final String methodKey;
    private final int upstreamStatus;

    public UpstreamBusinessException(String methodKey, int upstreamStatus) {
        super("上游业务校验失败, method=" + methodKey + ", status=" + upstreamStatus);
        this.methodKey = methodKey;
        this.upstreamStatus = upstreamStatus;
    }
}
