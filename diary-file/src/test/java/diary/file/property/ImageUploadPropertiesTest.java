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

class ImageUploadPropertiesTest {

    private static final String CHUNK_SIZE = "diary.file.image-upload.chunk-size=262144";
    private static final String MAX_IMAGE_SIZE = "diary.file.image-upload.max-image-size=104857600";
    private static final String SESSION_TTL_HOURS = "diary.file.image-upload.session-ttl-hours=24";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldBindImageUploadProperties() {
        contextRunner.withPropertyValues(validProperties()).run(context -> {
            assertThat(context).hasNotFailed();

            ImageUploadProperties properties = context.getBean(ImageUploadProperties.class);
            assertThat(properties.getChunkSize()).isEqualTo(262_144);
            assertThat(properties.getMaxImageSize()).isEqualTo(104_857_600);
            assertThat(properties.getSessionTtlHours()).isEqualTo(24);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "diary.file.image-upload.chunk-size",
            "diary.file.image-upload.max-image-size",
            "diary.file.image-upload.session-ttl-hours"
    })
    void shouldFailWhenRequiredPropertyIsMissing(String missingProperty) {
        assertBindingFails(propertiesWithout(missingProperty));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "diary.file.image-upload.chunk-size=0",
            "diary.file.image-upload.max-image-size=0",
            "diary.file.image-upload.session-ttl-hours=0"
    })
    void shouldFailWhenPropertyIsNotPositive(String invalidProperty) {
        assertBindingFails(propertiesWithOverride(invalidProperty));
    }

    private void assertBindingFails(String... properties) {
        contextRunner.withPropertyValues(properties).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(BindValidationException.class);
        });
    }

    private String[] validProperties() {
        return new String[]{CHUNK_SIZE, MAX_IMAGE_SIZE, SESSION_TTL_HOURS};
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
    @EnableConfigurationProperties(ImageUploadProperties.class)
    static class TestConfiguration {
    }
}
