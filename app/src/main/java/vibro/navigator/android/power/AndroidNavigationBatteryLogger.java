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
import vibro.navigator.nav.power.NavigationChargeCounterValidation;
import vibro.navigator.nav.power.NavigationBatterySnapshot;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;

/** Device-wide measurements, sampled only when an existing location callback wakes the service. */
public final class AndroidNavigationBatteryLogger {
    private static final long SAMPLE_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);
    private static final String UNKNOWN = "unknown";
    private static final int UNKNOWN_VALUE = Integer.MIN_VALUE;
    private final Context context;
    private long lastSampleMs = Long.MIN_VALUE;
    private final NavigationChargeCounterValidation counterValidation = new NavigationChargeCounterValidation();

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
            int level = propertyValue(battery, BatteryManager.BATTERY_PROPERTY_CAPACITY);
            int counter = propertyValue(battery, BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
            return "powerReadings=unvalidated levelPercent=" + availableValue(level)
                    + " levelPercentSource=capacity_property " + levelComparison(level, intent)
                    + " chargeCounterUah=" + availableValue(counter)
                    + " chargeCounterStatus=" + counterStatus(counter, level, intent)
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

    private String counterStatus(int counter, int level, Intent intent) {
        boolean charging = intent != null && AndroidBatterySnapshotReader.isCharging(intent);
        if (!NavigationBatterySnapshot.of(counter, -1).hasChargeCounter()) {
            return counterValidation.record(counter, charging);
        }
        return counterValidation.record(NavigationBatterySnapshot.of(counter, level,
                AndroidElapsedRealtimeClock.INSTANCE.elapsedRealtimeMs()), charging);
    }

    private static String levelComparison(int propertyLevel, Intent intent) {
        int broadcastLevel = broadcastLevelPercent(intent);
        boolean comparable = isPercent(propertyLevel) && isPercent(broadcastLevel);
        return "broadcastLevelPercent=" + availableValue(broadcastLevel)
                + " levelDifferencePercent=" + (comparable ? propertyLevel - broadcastLevel : UNKNOWN);
    }

    private static int broadcastLevelPercent(Intent intent) {
        if (intent == null) {
            return UNKNOWN_VALUE;
        }
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return level >= 0 && scale > 0 && level <= scale ? Math.round(level * 100f / scale) : UNKNOWN_VALUE;
    }

    private static boolean isPercent(int value) {
        return value >= 0 && value <= 100;
    }

    @NonNull
    private static String property(@Nullable BatteryManager battery, int property) {
        return availableValue(propertyValue(battery, property));
    }

    private static String availableValue(int value) {
        return value == UNKNOWN_VALUE ? UNKNOWN : Integer.toString(value);
    }

    private static int propertyValue(@Nullable BatteryManager battery, int property) {
        if (battery == null) {
            return UNKNOWN_VALUE;
        }
        try {
            return battery.getIntProperty(property);
        } catch (RuntimeException e) {
            return UNKNOWN_VALUE;
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
