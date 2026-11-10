package diary.file.property;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FileMqPropertiesTest {

    private static final String PREFIX = "diary.file.rocketmq.";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldBindAllProperties() {
        contextRunner.withPropertyValues(validProperties()).run(context -> {
            assertThat(context).hasNotFailed();

            FileMqProperties properties = context.getBean(FileMqProperties.class);
            assertThat(properties.getEventTopic()).isEqualTo("diary-file-event");
            assertThat(properties.getImageUploadedTag()).isEqualTo("IMAGE_UPLOADED");
            assertThat(properties.getImageReadyConsumerGroup()).isEqualTo("diary-file-image-ready-consumer");
            assertThat(properties.getPublisherBatchSize()).isEqualTo(20);
            assertThat(properties.getPublisherIntervalMs()).isEqualTo(10_000);
            assertThat(properties.getOutboxSendingTimeoutSeconds()).isEqualTo(60);
            assertThat(properties.getOutboxMaxRetries()).isEqualTo(5);
            assertThat(properties.getSentRetentionDays()).isEqualTo(7);
            assertThat(properties.getCleanBatchSize()).isEqualTo(500);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "event-topic",
            "image-uploaded-tag",
            "image-ready-consumer-group"
    })
    void shouldFailWhenRequiredTextPropertyIsMissing(String propertyName) {
        assertBindingFails(propertiesWithout(propertyName));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "publisher-batch-size",
            "publisher-interval-ms",
            "outbox-sending-timeout-seconds",
            "outbox-max-retries",
            "sent-retention-days",
            "clean-batch-size"
    })
    void shouldFailWhenRequiredNumericPropertyIsMissing(String propertyName) {
        assertBindingFails(propertiesWithout(propertyName));
    }

    @ParameterizedTest
    @CsvSource({
            "publisher-batch-size, 0",
            "publisher-interval-ms, 99",
            "outbox-sending-timeout-seconds, 0",
            "outbox-max-retries, -1",
            "sent-retention-days, 0",
            "clean-batch-size, 0"
    })
    void shouldFailWhenNumericPropertyIsOutOfRange(String propertyName, String invalidValue) {
        assertBindingFails(propertiesWithOverride(propertyName, invalidValue));
    }

    @Test
    void shouldAllowZeroOutboxRetries() {
        contextRunner.withPropertyValues(propertiesWithOverride("outbox-max-retries", "0"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(FileMqProperties.class).getOutboxMaxRetries()).isZero();
                });
    }

    private void assertBindingFails(String[] properties) {
        contextRunner.withPropertyValues(properties).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(BindValidationException.class);
        });
    }

    private String[] propertiesWithout(String propertyName) {
        String propertyPrefix = PREFIX + propertyName + "=";
        return Arrays.stream(validProperties())
                .filter(property -> !property.startsWith(propertyPrefix))
                .toArray(String[]::new);
    }

    private String[] propertiesWithOverride(String propertyName, String value) {
        return Stream.concat(
                        Arrays.stream(propertiesWithout(propertyName)),
                        Stream.of(PREFIX + propertyName + "=" + value)
                )
                .toArray(String[]::new);
    }

    private String[] validProperties() {
        return new String[]{
                PREFIX + "event-topic=diary-file-event",
                PREFIX + "image-uploaded-tag=IMAGE_UPLOADED",
                PREFIX + "image-ready-consumer-group=diary-file-image-ready-consumer",
                PREFIX + "publisher-batch-size=20",
                PREFIX + "publisher-interval-ms=10000",
                PREFIX + "outbox-sending-timeout-seconds=60",
                PREFIX + "outbox-max-retries=5",
                PREFIX + "sent-retention-days=7",
                PREFIX + "clean-batch-size=500"
        };
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FileMqProperties.class)
    static class TestConfiguration {
    }
}
