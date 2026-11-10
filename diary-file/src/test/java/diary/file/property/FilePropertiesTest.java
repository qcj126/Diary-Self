package diary.file.property;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class FilePropertiesTest {

    private static final String PART_SIZE = "diary.file.part.part-size=10485760";
    private static final String FILE_THRESHOLD = "diary.file.part.file-threshold=104857600";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldBindPartProperties() {
        contextRunner.withPropertyValues(PART_SIZE, FILE_THRESHOLD).run(context -> {
            assertThat(context).hasNotFailed();

            FileProperties properties = context.getBean(FileProperties.class);
            assertThat(properties.getPartSize()).isEqualTo(10_485_760L);
            assertThat(properties.getFileThreshold()).isEqualTo(104_857_600L);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            FILE_THRESHOLD,
            PART_SIZE
    })
    void shouldFailWhenRequiredPropertyIsMissing(String remainingProperty) {
        assertBindingFails(remainingProperty);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "diary.file.part.part-size=0",
            "diary.file.part.file-threshold=0"
    })
    void shouldFailWhenPropertyIsNotPositive(String invalidProperty) {
        String validCompanion = invalidProperty.contains("part-size") ? FILE_THRESHOLD : PART_SIZE;
        assertBindingFails(invalidProperty, validCompanion);
    }

    private void assertBindingFails(String... properties) {
        contextRunner.withPropertyValues(properties).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(BindValidationException.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FileProperties.class)
    static class TestConfiguration {
    }
}
