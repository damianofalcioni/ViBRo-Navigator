package vibro.navigator.android.power;

import android.content.Context;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.power.NavigationWakeLock;

public final class AndroidPartialWakeLock implements NavigationWakeLock {

    private static final String TAG = "NavWakeLock";

    @NonNull
    private final Context context;

    public AndroidPartialWakeLock(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @Nullable
    @Override
    public HeldWakeLock acquire(@NonNull String wakeLockTag, long timeoutMs) {
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
            return new AndroidHeldWakeLock(wakeLock, wakeLockTag);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to acquire wake lock tag=" + wakeLockTag + " timeoutMs=" + timeoutMs, e);
            return null;
        }
    }

    private static final class AndroidHeldWakeLock implements HeldWakeLock {
        @NonNull
        private final PowerManager.WakeLock wakeLock;
        @NonNull
        private final String wakeLockTag;

        AndroidHeldWakeLock(@NonNull PowerManager.WakeLock wakeLock, @NonNull String wakeLockTag) {
            this.wakeLock = wakeLock;
            this.wakeLockTag = wakeLockTag;
        }

        @Override
        public void close() {
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
}
