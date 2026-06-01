package vibro.navigator.nav.power;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import androidx.annotation.NonNull;

import org.junit.Test;

public class NavigationWakeLockControllerTest {

    private static final String WAKE_LOCK_TAG = "route";
    private static final String WORK_RESULT = "done";

    @Test
    public void runWithWakeLock_releasesWakeLockAfterSuccessfulWork() throws Exception {
        FakeWakeLock wakeLock = new FakeWakeLock();
        NavigationWakeLockController controller = new NavigationWakeLockController(wakeLock);

        String result = controller.runWithWakeLock(WAKE_LOCK_TAG, 100L, () -> WORK_RESULT);

        assertEquals(WORK_RESULT, result);
        assertEquals(WAKE_LOCK_TAG, wakeLock.acquiredTag);
        assertEquals(100L, wakeLock.acquiredTimeoutMs);
        assertEquals(1, wakeLock.heldWakeLock.closeCalls);
    }

    @Test
    public void runWithWakeLock_releasesWakeLockAfterFailedWork() {
        FakeWakeLock wakeLock = new FakeWakeLock();
        NavigationWakeLockController controller = new NavigationWakeLockController(wakeLock);
        IllegalStateException expected = new IllegalStateException("boom");

        try {
            controller.runWithWakeLock(WAKE_LOCK_TAG, 100L, () -> {
                throw expected;
            });
            fail("Expected route work failure");
        } catch (Exception e) {
            assertSame(expected, e);
        }

        assertEquals(1, wakeLock.heldWakeLock.closeCalls);
    }

    @Test
    public void runWithWakeLock_runsWorkWhenAcquireFails() throws Exception {
        FakeWakeLock wakeLock = new FakeWakeLock();
        wakeLock.returnNull = true;
        NavigationWakeLockController controller = new NavigationWakeLockController(wakeLock);

        String result = controller.runWithWakeLock(WAKE_LOCK_TAG, 100L, () -> WORK_RESULT);

        assertEquals(WORK_RESULT, result);
        assertNull(wakeLock.returnedWakeLock);
    }

    private static final class FakeWakeLock implements NavigationWakeLock {
        private final FakeHeldWakeLock heldWakeLock = new FakeHeldWakeLock();
        private boolean returnNull;
        private String acquiredTag;
        private long acquiredTimeoutMs;
        private HeldWakeLock returnedWakeLock;

        @Override
        public HeldWakeLock acquire(@NonNull String wakeLockTag, long timeoutMs) {
            acquiredTag = wakeLockTag;
            acquiredTimeoutMs = timeoutMs;
            returnedWakeLock = returnNull ? null : heldWakeLock;
            return returnedWakeLock;
        }
    }

    private static final class FakeHeldWakeLock implements NavigationWakeLock.HeldWakeLock {
        private int closeCalls;

        @Override
        public void close() {
            closeCalls++;
        }
    }
}
