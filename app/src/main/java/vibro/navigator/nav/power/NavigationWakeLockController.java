package vibro.navigator.nav.power;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Callable;

public final class NavigationWakeLockController {

    @NonNull
    private final NavigationWakeLock wakeLock;

    public NavigationWakeLockController(@NonNull NavigationWakeLock wakeLock) {
        this.wakeLock = wakeLock;
    }

    public <T> T runWithWakeLock(
            @NonNull String wakeLockTag,
            long timeoutMs,
            @NonNull Callable<T> work
    ) throws Exception {
        NavigationWakeLock.HeldWakeLock scopedWakeLock = wakeLock.acquire(wakeLockTag, timeoutMs);
        try {
            return work.call();
        } finally {
            release(scopedWakeLock);
        }
    }

    private static void release(@Nullable NavigationWakeLock.HeldWakeLock wakeLock) {
        if (wakeLock != null) {
            wakeLock.close();
        }
    }
}
