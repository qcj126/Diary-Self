package diary.file.enums;

import diary.common.exception.ParamIllegalException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUploadSessionStatusEnumTest {

    @Test
    void shouldResolveEveryStatusByCode() {
        for (ImageUploadSessionStatusEnum status : ImageUploadSessionStatusEnum.values()) {
            assertThat(ImageUploadSessionStatusEnum.fromCode(status.getCode())).isSameAs(status);
        }
    }

    @Test
    void shouldRejectNullAndUnknownCodes() {
        assertThatThrownBy(() -> ImageUploadSessionStatusEnum.fromCode(null))
                .isInstanceOf(ParamIllegalException.class);
        assertThatThrownBy(() -> ImageUploadSessionStatusEnum.fromCode(-1))
                .isInstanceOf(ParamIllegalException.class);
    }
}
