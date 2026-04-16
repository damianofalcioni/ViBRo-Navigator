package vibro.navigator.nav;

import android.content.Context;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.util.AppLogger;

import java.util.concurrent.Callable;

final class NavigationWakeLockController {

    private static final String TAG = "NavWakeLock";

    private final Context context;

    NavigationWakeLockController(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    <T> T runWithWakeLock(
            @NonNull String wakeLockTag,
            long timeoutMs,
            @NonNull Callable<T> work
    ) throws Exception {
        PowerManager.WakeLock scopedWakeLock = tryAcquire(wakeLockTag, timeoutMs);
        try {
            return work.call();
        } finally {
            release(scopedWakeLock, wakeLockTag);
        }
    }

    @Nullable
    private PowerManager.WakeLock tryAcquire(@NonNull String wakeLockTag, long timeoutMs) {
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                AppLogger.w(TAG, "PowerManager unavailable, wake lock not acquired tag=" + wakeLockTag);
                return null;
            }
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire(Math.max(1L, timeoutMs));
            AppLogger.d(TAG, "Wake lock acquired tag=" + wakeLockTag + " timeoutMs=" + timeoutMs);
            return wakeLock;
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to acquire wake lock tag=" + wakeLockTag + " timeoutMs=" + timeoutMs, e);
            return null;
        }
    }

    private void release(@Nullable PowerManager.WakeLock wakeLock, @NonNull String wakeLockTag) {
        if (wakeLock == null) {
            return;
        }
        try {
            if (wakeLock.isHeld()) {
                wakeLock.release();
                AppLogger.d(TAG, "Wake lock released tag=" + wakeLockTag);
            }
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to release wake lock tag=" + wakeLockTag, e);
        }
    }
}
