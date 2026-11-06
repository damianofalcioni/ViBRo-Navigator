package vibro.navigator.android.power;

import android.content.Context;
import android.os.PowerManager;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;

/** Device-wide measurements, sampled only when an existing location callback wakes the service. */
public final class AndroidNavigationBatteryLogger {
    private static final long SAMPLE_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);
    private final Context context;
    private long lastSampleMs = Long.MIN_VALUE;
    private AndroidBatterySnapshotReader snapshotReader;

    public AndroidNavigationBatteryLogger(@NonNull Context context) {
        this.context = context;
        snapshotReader = new AndroidBatterySnapshotReader(context, AndroidElapsedRealtimeClock.INSTANCE, false);
    }

    public void useSnapshotReader(AndroidBatterySnapshotReader reader) {
        snapshotReader = reader;
    }

    public void record(long nowMs, @NonNull String activeProviders, long requestedIntervalMs) {
        if (!shouldSample(nowMs, AppLogger.isLoggingEnabled())) {
            return;
        }
        AppLogger.d("NavBattery", "Battery sample scope=device elapsedRealtimeMs=" + nowMs
                + " activeProviders=" + activeProviders + " requestedIntervalMs=" + requestedIntervalMs
                + " " + readMeasurements());
    }

    boolean shouldSample(long nowMs, boolean enabled) {
        if (!enabled) {
            lastSampleMs = Long.MIN_VALUE;
            return false;
        }
        if (lastSampleMs != Long.MIN_VALUE && nowMs >= lastSampleMs
                && nowMs - lastSampleMs < SAMPLE_INTERVAL_MS) {
            return false;
        }
        lastSampleMs = nowMs;
        return true;
    }

    @NonNull
    String readMeasurements() {
        try {
            PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return snapshotReader.sampleDiagnostics()
                    + " screenInteractive=" + (power == null ? "unknown" : power.isInteractive());
        } catch (RuntimeException e) {
            return "measurements=unavailable error=" + e.getClass().getSimpleName();
        }
    }
}
