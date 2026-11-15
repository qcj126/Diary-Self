package diary.file.property;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ThumbnailPropertiesTest {

    private static final String MAX_WIDTH = "diary.file.thumbnail.max-width=480";
    private static final String MAX_HEIGHT = "diary.file.thumbnail.max-height=480";
    private static final String JPEG_QUALITY = "diary.file.thumbnail.jpeg-quality=0.85";
    private static final String MAX_PIXELS = "diary.file.thumbnail.max-pixels=40000000";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldBindThumbnailProperties() {
        contextRunner.withPropertyValues(validProperties()).run(context -> {
            assertThat(context).hasNotFailed();

            ThumbnailProperties properties = context.getBean(ThumbnailProperties.class);
            assertThat(properties.getMaxWidth()).isEqualTo(480);
            assertThat(properties.getMaxHeight()).isEqualTo(480);
            assertThat(properties.getJpegQuality()).isEqualTo(0.85d);
            assertThat(properties.getMaxPixels()).isEqualTo(40_000_000);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "diary.file.thumbnail.max-width",
            "diary.file.thumbnail.max-height",
            "diary.file.thumbnail.jpeg-quality",
            "diary.file.thumbnail.max-pixels"
    })
    void shouldFailWhenRequiredPropertyIsMissing(String missingProperty) {
        assertBindingFails(propertiesWithout(missingProperty));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "diary.file.thumbnail.max-width=0",
            "diary.file.thumbnail.max-height=0",
            "diary.file.thumbnail.max-pixels=0"
    })
    void shouldFailWhenSizePropertyIsNotPositive(String invalidProperty) {
        assertBindingFails(propertiesWithOverride(invalidProperty));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "diary.file.thumbnail.jpeg-quality=-0.01",
            "diary.file.thumbnail.jpeg-quality=1.01"
    })
    void shouldFailWhenJpegQualityIsOutsideSupportedRange(String invalidProperty) {
        assertBindingFails(propertiesWithOverride(invalidProperty));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "diary.file.thumbnail.jpeg-quality=0",
            "diary.file.thumbnail.jpeg-quality=1"
    })
    void shouldAllowJpegQualityBoundaryValues(String boundaryProperty) {
        contextRunner.withPropertyValues(propertiesWithOverride(boundaryProperty))
                .run(context -> assertThat(context).hasNotFailed());
    }

    private void assertBindingFails(String... properties) {
        contextRunner.withPropertyValues(properties).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(BindValidationException.class);
        });
    }

    private String[] validProperties() {
        return new String[]{MAX_WIDTH, MAX_HEIGHT, JPEG_QUALITY, MAX_PIXELS};
    }

    private String[] propertiesWithout(String missingProperty) {
        return Arrays.stream(validProperties())
                .filter(property -> !property.startsWith(missingProperty + "="))
                .toArray(String[]::new);
    }

    private String[] propertiesWithOverride(String override) {
        String propertyName = override.substring(0, override.indexOf('='));
        String[] remainingProperties = propertiesWithout(propertyName);
        String[] properties = Arrays.copyOf(remainingProperties, remainingProperties.length + 1);
        properties[remainingProperties.length] = override;
        return properties;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ThumbnailProperties.class)
    static class TestConfiguration {
    }
}
