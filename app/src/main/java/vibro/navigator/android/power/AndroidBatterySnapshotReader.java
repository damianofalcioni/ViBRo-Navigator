package vibro.navigator.android.power;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.concurrent.TimeUnit;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.power.NavigationBatterySnapshot;
import vibro.navigator.nav.power.NavigationBatterySnapshotCache;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;

public final class AndroidBatterySnapshotReader {
    private static final String TAG = "NavBattery";
    private static final int UNKNOWN = Integer.MIN_VALUE;
    // Battery broadcasts are the primary refresh path; this only recovers from missed broadcasts.
    private static final long MAXIMUM_SNAPSHOT_AGE_MS = TimeUnit.MINUTES.toMillis(5L);

    @NonNull
    private final Context context;
    @NonNull
    private final NavigationBatterySnapshotCache cache;
    @NonNull
    private final BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context receiverContext, Intent intent) {
            record(intent);
        }
    };
    private boolean observingBatteryChanges;

    public AndroidBatterySnapshotReader(@NonNull Context context) {
        this(context, AndroidElapsedRealtimeClock.INSTANCE);
    }

    AndroidBatterySnapshotReader(
            @NonNull Context context,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        this.context = context.getApplicationContext();
        cache = new NavigationBatterySnapshotCache(elapsedRealtimeClock, MAXIMUM_SNAPSHOT_AGE_MS);
        start();
    }

    public void start() {
        if (observingBatteryChanges) {
            return;
        }
        try {
            Intent stickyBatteryIntent = ContextCompat.registerReceiver(
                    context,
                    batteryChangedReceiver,
                    batteryFilter(),
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
            observingBatteryChanges = true;
            if (stickyBatteryIntent != null) {
                record(stickyBatteryIntent);
            }
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to observe battery changes", e);
        }
    }

    public void stop() {
        if (!observingBatteryChanges) {
            return;
        }
        try {
            context.unregisterReceiver(batteryChangedReceiver);
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to stop observing battery changes", e);
        } finally {
            observingBatteryChanges = false;
        }
    }

    @NonNull
    public NavigationBatterySnapshot read() {
        if (cache.needsRefresh()) {
            record(batteryIntent());
        }
        return cache.current();
    }

    @Nullable
    private Intent batteryIntent() {
        try {
            return context.registerReceiver(null, batteryFilter());
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to query battery snapshot", e);
            return null;
        }
    }

    private void record(@Nullable Intent batteryIntent) {
        try {
            cache.record(snapshotFrom(batteryIntent));
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to read battery snapshot", e);
            cache.record(NavigationBatterySnapshot.unavailable());
        }
    }

    @NonNull
    private NavigationBatterySnapshot snapshotFrom(@Nullable Intent batteryIntent) {
        if (batteryIntent == null || isCharging(batteryIntent)) {
            return NavigationBatterySnapshot.unavailable();
        }
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return NavigationBatterySnapshot.of(
                chargeCounterMicroAmpHours(batteryManager),
                levelPercent(batteryManager, batteryIntent)
        );
    }

    @NonNull
    private static IntentFilter batteryFilter() {
        return new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
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
