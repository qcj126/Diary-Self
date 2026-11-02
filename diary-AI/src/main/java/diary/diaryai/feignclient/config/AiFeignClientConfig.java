package diary.diaryai.feignclient.config;

import diary.diaryai.exceptions.UpstreamBusinessException;
import diary.diaryai.exceptions.UpstreamCallException;
import diary.diaryai.exceptions.UpstreamUnavailableException;
import feign.Response;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

/**
 * diary-AI 调用内部业务服务时共用的 Feign 行为。
 *
 * <p>本类不标注 {@code @Configuration} 或 {@code @Component}，仅由显式声明
 * {@code configuration = AiFeignClientConfig.class} 的 Feign Client 加载，防止意外影响其他服务的 Client。</p>
 *
 * <p>连接与读取超时由 Nacos 的 {@code spring.cloud.openfeign.client.config} 配置；
 * 此处只定义重试策略与非 2xx 响应的业务异常分类。</p>
 */
public class AiFeignClientConfig {

    /**
     * 禁用 Feign 自动重试。
     *
     * <p>上游查询失败时由调用方返回明确错误，客户端可携带同一 {@code clientRequestId} 重试。
     * 避免一次用户提交在不可控的网络异常下产生多次同步服务调用。</p>
     */
    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY;
    }

    /**
     * 将上游 HTTP 非 2xx 响应转换为调用层可区分处理的异常。
     *
     * <p>不读取、透传上游响应体，避免把其他服务的内部错误或敏感字段暴露给当前接口调用方。</p>
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            int status = response.status();
            return switch (status) {
                // 提交目标不存在或请求字段不被上游接受，客户端立即重试没有意义。
                case 400, 404 -> new UpstreamBusinessException(methodKey, status);
                // 鉴权失败通常是服务间调用配置错误，不应伪装为终端用户未登录。
                case 401, 403 -> new UpstreamCallException(methodKey, status);
                // 限流、网关错误和服务不可用均属于暂时性依赖故障。
                case 408, 429, 502, 503, 504 -> new UpstreamUnavailableException(methodKey, status);
                default -> new UpstreamCallException(methodKey, status);
            };
        };
    }
}
