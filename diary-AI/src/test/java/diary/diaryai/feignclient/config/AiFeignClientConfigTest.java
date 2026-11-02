package diary.diaryai.feignclient.config;

import diary.diaryai.exceptions.UpstreamBusinessException;
import diary.diaryai.exceptions.UpstreamCallException;
import diary.diaryai.exceptions.UpstreamUnavailableException;
import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 锁定 Feign 非 2xx 状态码到领域异常的映射，避免后续调整时错误地扩大重试范围。
 */
class AiFeignClientConfigTest {
    private final ErrorDecoder errorDecoder = new AiFeignClientConfig().errorDecoder();

    @Test
    void shouldClassifyInvalidOrMissingBusinessObjectAsPermanentFailure() {
        Exception exception = errorDecoder.decode("DietClient#getDietFoodName", response(404));

        assertInstanceOf(UpstreamBusinessException.class, exception);
    }

    @Test
    void shouldClassifyRateLimitedOrUnavailableDependencyAsTransientFailure() {
        Exception exception = errorDecoder.decode("LoveRecordClient#getLoveRecordAiSnapshot", response(503));

        assertInstanceOf(UpstreamUnavailableException.class, exception);
    }

    @Test
    void shouldClassifyUnexpectedOrAuthenticationErrorAsGatewayFailure() {
        Exception exception = errorDecoder.decode("RecipeClient#getRecipeTitle", response(401));

        assertInstanceOf(UpstreamCallException.class, exception);
    }

    /**
     * 构造最小 Feign Response，仅验证状态码分类，不依赖真实 HTTP 服务或 Nacos。
     */
    private Response response(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://upstream.example.test/query",
                Map.of(),
                null,
                StandardCharsets.UTF_8
        );
        return Response.builder()
                .status(status)
                .reason("test")
                .headers(Map.of())
                .request(request)
                .build();
    }
}
