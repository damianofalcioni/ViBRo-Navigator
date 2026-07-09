package vibro.navigator.android.power;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.power.NavigationBatterySnapshot;

public final class AndroidBatterySnapshotReader {
    private static final String TAG = "NavBattery";
    private static final int UNKNOWN = Integer.MIN_VALUE;

    @NonNull
    private final Context context;

    public AndroidBatterySnapshotReader(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    public NavigationBatterySnapshot read() {
        try {
            Intent batteryIntent = batteryIntent();
            if (batteryIntent == null || isCharging(batteryIntent)) {
                return NavigationBatterySnapshot.unavailable();
            }
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            return NavigationBatterySnapshot.of(
                    chargeCounterMicroAmpHours(batteryManager),
                    levelPercent(batteryManager, batteryIntent)
            );
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to read battery snapshot", e);
            return NavigationBatterySnapshot.unavailable();
        }
    }

    @Nullable
    private Intent batteryIntent() {
        return context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private static int chargeCounterMicroAmpHours(@Nullable BatteryManager batteryManager) {
        return batteryManager == null
                ? UNKNOWN
                : batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
    }

    private static int levelPercent(@Nullable BatteryManager batteryManager, @NonNull Intent batteryIntent) {
        int propertyLevel = batteryManager == null
                ? UNKNOWN
                : batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return isPercent(propertyLevel) ? propertyLevel : levelPercentFromIntent(batteryIntent);
    }

    private static int levelPercentFromIntent(@NonNull Intent batteryIntent) {
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, UNKNOWN);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, UNKNOWN);
        if (level < 0 || scale <= 0) {
            return UNKNOWN;
        }
        return Math.round((level * 100f) / scale);
    }

    private static boolean isCharging(@NonNull Intent batteryIntent) {
        int status = batteryIntent.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN
        );
        return batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                || status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
    }

    private static boolean isPercent(int value) {
        return value >= 0 && value <= 100;
    }
}
