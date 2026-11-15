package diary.file.enums;

import diary.common.exception.CustomException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageTypeEnumTest {

    @Test
    void shouldResolveEveryBusinessTypeByCode() {
        for (ImageTypeEnum type : ImageTypeEnum.values()) {
            assertThat(ImageTypeEnum.fromCode(type.getCode())).isSameAs(type);
            assertThat(ImageTypeEnum.getCode(type.getCode())).isEqualTo(type.getCode());
            assertThat(ImageTypeEnum.getType(type.getCode())).isEqualTo(type.getDescription());
        }
    }

    @Test
    void shouldRejectNullAndUnknownCodes() {
        assertThatThrownBy(() -> ImageTypeEnum.fromCode(null))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> ImageTypeEnum.fromCode(-1))
                .isInstanceOf(CustomException.class);
    }
}
