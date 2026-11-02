package diary.diaryai.guard;

import diary.diaryai.exceptions.AiProviderCircuitOpenException;
import diary.diaryai.exceptions.AiProviderCallException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * DashScope 同步调用的统一熔断入口。
 *
 * <p>初版有意只使用一个 {@value #CIRCUIT_BREAKER_NAME} 熔断器：Qwen 与 DeepSeek
 * 当前都通过同一 DashScope/百炼入口访问。这样只需要在四个策略类的 SDK 调用点包一层，
 * 不引入新的线程池或并发控制；本地与全局并发仍分别由现有 Semaphore 和 Redis permit 负责。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelCallGuard {
    public static final String CIRCUIT_BREAKER_NAME = "dashscope";

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private CircuitBreaker circuitBreaker;

    @PostConstruct
    void initialize() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        circuitBreaker.getEventPublisher().onStateTransition(event -> log.warn(
                "DashScope 熔断器状态变化，name={}, transition={}",
                event.getCircuitBreakerName(), event.getStateTransition()));
        log.info("DashScope 熔断器已初始化，name={}", CIRCUIT_BREAKER_NAME);
    }

    /**
     * 在不改变 SDK 同步调用方式的前提下记录成功、失败和慢调用。
     *
     * <p>OPEN 状态不会执行 {@code supplier}，因此不会继续向供应商发送请求或产生调用成本。</p>
     */
    public <T> T execute(String model, CheckedSupplier<T> supplier) {
        try {
            // Resilience4j 使用的是标准 Supplier；SDK 的受检异常在这里统一转换。
            return circuitBreaker.executeSupplier(() -> invokeSdk(model, supplier));
        } catch (CallNotPermittedException exception) {
            log.warn("DashScope 熔断器已打开，拒绝模型调用，model={}, breaker={}",
                    model, CIRCUIT_BREAKER_NAME);
            throw new AiProviderCircuitOpenException(model, exception);
        }
    }

    private <T> T invokeSdk(String model, CheckedSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("DashScope SDK 调用抛出受检异常，model={}, exceptionType={}",
                    model, exception.getClass().getSimpleName());
            throw new AiProviderCallException(model, exception);
        }
    }

    /**
     * DashScope 的 {@code call()} 会声明受检异常；使用该接口避免每个策略类重复 try/catch。
     */
    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
