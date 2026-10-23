package vibro.navigator.android.power;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import vibro.navigator.logging.AppLogger;

/** Device-wide measurements, sampled only when an existing location callback wakes the service. */
public final class AndroidNavigationBatteryLogger {
    private static final long SAMPLE_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);
    private static final String UNKNOWN = "unknown";
    private final Context context;
    private long lastSampleMs = Long.MIN_VALUE;

    public AndroidNavigationBatteryLogger(@NonNull Context context) {
        this.context = context;
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
            BatteryManager battery = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            return "powerReadings=unvalidated levelPercent=" + property(battery, BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    + " chargeCounterUah=" + property(battery, BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                    + " currentNowUa=" + property(battery, BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    + " currentAverageUa=" + property(battery, BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
                    + " voltageMv=" + extra(intent, BatteryManager.EXTRA_VOLTAGE)
                    + " temperatureTenthsC=" + extra(intent, BatteryManager.EXTRA_TEMPERATURE)
                    + " plugged=" + extra(intent, BatteryManager.EXTRA_PLUGGED)
                    + " status=" + extra(intent, BatteryManager.EXTRA_STATUS)
                    + " screenInteractive=" + (power == null ? UNKNOWN : power.isInteractive());
        } catch (RuntimeException e) {
            return "measurements=unavailable error=" + e.getClass().getSimpleName();
        }
    }

    @NonNull
    private static String property(@Nullable BatteryManager battery, int property) {
        if (battery == null) {
            return UNKNOWN;
        }
        try {
            int value = battery.getIntProperty(property);
            return value == Integer.MIN_VALUE ? UNKNOWN : Integer.toString(value);
        } catch (RuntimeException e) {
            return UNKNOWN;
        }
    }

    @NonNull
    private static String extra(@Nullable Intent intent, @NonNull String name) {
        if (intent == null || !intent.hasExtra(name)) {
            return UNKNOWN;
        }
        return Integer.toString(intent.getIntExtra(name, 0));
    }
}
