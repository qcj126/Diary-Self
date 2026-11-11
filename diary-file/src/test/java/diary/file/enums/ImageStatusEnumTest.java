package diary.file.enums;

import diary.common.exception.ParamIllegalException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageStatusEnumTest {

    @Test
    @DisplayName("枚举遍历：打印所有状态")
    void testEnumValues() {
        for (ImageStatusEnum e : ImageStatusEnum.values()) {
            System.out.println(e.getCode() + " -> " + e.getDescription());
            assertNotNull(e.getDescription());
        }
        assertEquals(6, ImageStatusEnum.values().length);
    }

    @Test
    @DisplayName("isValid：合法编码返回 true")
    void testIsValid_true() {
        assertTrue(ImageStatusEnum.isValid(1000));
        assertTrue(ImageStatusEnum.isValid(1100));
        assertTrue(ImageStatusEnum.isValid(1200));
        assertTrue(ImageStatusEnum.isValid(1210));
        assertTrue(ImageStatusEnum.isValid(1220));
        assertTrue(ImageStatusEnum.isValid(1230));
    }

    @Test
    @DisplayName("isValid：非法编码返回 false")
    void testIsValid_false() {
        assertFalse(ImageStatusEnum.isValid(0));
        assertFalse(ImageStatusEnum.isValid(-1));
        assertFalse(ImageStatusEnum.isValid(9999));
    }

    @Test
    @DisplayName("fromCode：合法编码返回对应枚举")
    void testFromCode_success() {
        assertEquals(ImageStatusEnum.READY, ImageStatusEnum.fromCode(1000));
        assertEquals(ImageStatusEnum.UPLOAD_FAILED, ImageStatusEnum.fromCode(1100));
        assertEquals(ImageStatusEnum.PENDING_UPLOAD, ImageStatusEnum.fromCode(1200));
        assertEquals(ImageStatusEnum.UPLOADING, ImageStatusEnum.fromCode(1210));
        assertEquals(ImageStatusEnum.UPLOADED, ImageStatusEnum.fromCode(1220));
        assertEquals(ImageStatusEnum.RETRY_WAIT, ImageStatusEnum.fromCode(1230));
    }

    @Test
    @DisplayName("fromCode：非法编码抛 ParamIllegalException")
    void testFromCode_throw() {
        assertThrows(ParamIllegalException.class, () -> ImageStatusEnum.fromCode(9999));
    }

    @Test
    @DisplayName("isTerminal：仅成功和失败属于最终状态")
    void isTerminal_onlyReadyAndFailed() {
        assertTrue(ImageStatusEnum.READY.isTerminal());
        assertTrue(ImageStatusEnum.UPLOAD_FAILED.isTerminal());

        assertFalse(ImageStatusEnum.PENDING_UPLOAD.isTerminal());
        assertFalse(ImageStatusEnum.UPLOADING.isTerminal());
        assertFalse(ImageStatusEnum.UPLOADED.isTerminal());
        assertFalse(ImageStatusEnum.RETRY_WAIT.isTerminal());
    }

    @Test
    @DisplayName("isSuccess：只有 READY 表示上传成功")
    void isSuccess_onlyReady() {
        assertTrue(ImageStatusEnum.READY.isSuccess());

        for (ImageStatusEnum status : ImageStatusEnum.values()) {
            if (status != ImageStatusEnum.READY) {
                assertFalse(status.isSuccess());
            }
        }
    }
}
