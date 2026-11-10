package diary.file.state;

import diary.file.enums.ImageStatusEnum;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static diary.file.enums.ImageStatusEnum.PENDING_UPLOAD;
import static diary.file.enums.ImageStatusEnum.READY;
import static diary.file.enums.ImageStatusEnum.RETRY_WAIT;
import static diary.file.enums.ImageStatusEnum.UPLOADED;
import static diary.file.enums.ImageStatusEnum.UPLOADING;
import static diary.file.enums.ImageStatusEnum.UPLOAD_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ImageStatusTransitionPolicyTest {

    private final ImageStatusTransitionPolicy policy = new ImageStatusTransitionPolicy();

    private static final Set<Transition> ALLOWED_TRANSITIONS = Set.of(
            new Transition(PENDING_UPLOAD, UPLOADING),
            new Transition(PENDING_UPLOAD, UPLOAD_FAILED),
            new Transition(UPLOADING, UPLOADED),
            new Transition(UPLOADING, RETRY_WAIT),
            new Transition(UPLOADING, UPLOAD_FAILED),
            new Transition(RETRY_WAIT, UPLOADING),
            new Transition(RETRY_WAIT, UPLOAD_FAILED),
            new Transition(UPLOADED, READY)
    );

    @Test
    void onlyDeclaredTransitionsAreAllowed() {
        for (ImageStatusEnum currentStatus : ImageStatusEnum.values()) {
            for (ImageStatusEnum targetStatus : ImageStatusEnum.values()) {
                Transition transition = new Transition(currentStatus, targetStatus);

                assertEquals(
                        ALLOWED_TRANSITIONS.contains(transition),
                        policy.canTransition(currentStatus, targetStatus),
                        () -> "状态迁移结果不符合约定: " + currentStatus + " -> " + targetStatus
                );
            }
        }
    }

    @Test
    void nullStatusIsRejected() {
        assertFalse(policy.canTransition(null, READY));
        assertFalse(policy.canTransition(PENDING_UPLOAD, null));
        assertFalse(policy.canTransition(null, null));
    }

    private record Transition(ImageStatusEnum currentStatus, ImageStatusEnum targetStatus) {
    }
}
