package com.vibenavigator.nav;

import android.content.Context;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.util.AppLogger;

final class NavigationWakeLockController {

    private static final String TAG = "NavWakeLock";
    private static final String WAKE_LOCK_TAG = "VibeNavigator:Nav";

    private final Context context;
    @Nullable
    private PowerManager.WakeLock wakeLock;

    NavigationWakeLockController(@NonNull Context context) {
        this.context = context;
    }

    void acquire() {
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                AppLogger.w(TAG, "PowerManager unavailable, wake lock not acquired");
                return;
            }
            if (wakeLock != null && wakeLock.isHeld()) {
                AppLogger.d(TAG, "Wake lock already held");
                return;
            }
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
            wakeLock.acquire();
            AppLogger.i(TAG, "Wake lock acquired");
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to acquire wake lock", e);
        }
    }

    void release() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                AppLogger.i(TAG, "Wake lock released");
            }
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to release wake lock", e);
        } finally {
            wakeLock = null;
        }
    }
}
