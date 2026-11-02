package diary.diaryai.guard;

import diary.diaryai.exceptions.AiProviderCircuitOpenException;
import diary.diaryai.exceptions.AiProviderCallException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiModelCallGuardTest {

    @Test
    void executesSupplierWhileCircuitIsClosed() {
        AiModelCallGuard guard = newGuard();

        String result = guard.execute("qwen-plus", () -> "ok");

        assertEquals("ok", result);
    }

    @Test
    void doesNotInvokeSupplierWhenCircuitIsOpen() {
        CircuitBreakerRegistry registry = testRegistry();
        AiModelCallGuard guard = new AiModelCallGuard(registry);
        guard.initialize();
        registry.circuitBreaker(AiModelCallGuard.CIRCUIT_BREAKER_NAME).transitionToOpenState();
        AtomicBoolean called = new AtomicBoolean(false);

        assertThrows(AiProviderCircuitOpenException.class,
                () -> guard.execute("deepseek-v4-pro", () -> {
                    called.set(true);
                    return "should-not-run";
                }));

        assertFalse(called.get(), "熔断打开时不应继续调用供应商 SDK");
    }

    @Test
    void convertsSdkCheckedExceptionToRetryableRuntimeException() {
        AiModelCallGuard guard = newGuard();
        Exception sdkException = new Exception("missing api key");

        AiProviderCallException exception = assertThrows(AiProviderCallException.class,
                () -> guard.execute("qwen-plus", () -> {
                    throw sdkException;
                }));

        assertSame(sdkException, exception.getCause());
    }

    private AiModelCallGuard newGuard() {
        AiModelCallGuard guard = new AiModelCallGuard(testRegistry());
        guard.initialize();
        return guard;
    }

    private CircuitBreakerRegistry testRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(1)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .build();
        return CircuitBreakerRegistry.of(config);
    }
}
