package diary.diaryai.exceptions;

import lombok.Getter;

/**
 * 上游服务限流、网关异常或暂时不可用；调用方可在稍后使用相同幂等键再次提交。
 */
@Getter
public class UpstreamUnavailableException extends RuntimeException {
    private final String methodKey;
    private final int upstreamStatus;

    public UpstreamUnavailableException(String methodKey, int upstreamStatus) {
        super("上游服务暂不可用, method=" + methodKey + ", status=" + upstreamStatus);
        this.methodKey = methodKey;
        this.upstreamStatus = upstreamStatus;
    }
}
