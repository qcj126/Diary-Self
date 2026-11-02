package diary.diaryai.exceptions;

import lombok.Getter;

/**
 * 上游服务返回了无法归类的协议、鉴权或服务端错误，需要由服务端排查调用链配置。
 */
@Getter
public class UpstreamCallException extends RuntimeException {
    private final String methodKey;
    private final int upstreamStatus;

    public UpstreamCallException(String methodKey, int upstreamStatus) {
        super("上游服务调用失败, method=" + methodKey + ", status=" + upstreamStatus);
        this.methodKey = methodKey;
        this.upstreamStatus = upstreamStatus;
    }
}
