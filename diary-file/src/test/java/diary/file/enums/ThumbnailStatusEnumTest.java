package diary.file.enums;

import diary.common.exception.ParamIllegalException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThumbnailStatusEnumTest {
    @Test
    void resolvesEveryPersistedCode() {
        for (ThumbnailStatusEnum status : ThumbnailStatusEnum.values()) {
            assertEquals(status, ThumbnailStatusEnum.fromCode(status.getCode()));
        }
    }

    @Test
    void rejectsUnknownCode() {
        assertThrows(ParamIllegalException.class, () -> ThumbnailStatusEnum.fromCode(99));
    }
}
