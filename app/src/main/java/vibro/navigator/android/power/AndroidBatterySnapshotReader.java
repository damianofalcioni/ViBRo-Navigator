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
import vibro.navigator.nav.power.NavigationBatteryBehaviorDetector;
import vibro.navigator.nav.power.NavigationBatteryBehavior;
import vibro.navigator.nav.time.ElapsedRealtimeClock;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;

public final class AndroidBatterySnapshotReader {
    private static final String TAG = "NavBattery";
    // Battery broadcasts are the primary refresh path; this only recovers from missed broadcasts.
    private static final long MAXIMUM_SNAPSHOT_AGE_MS = TimeUnit.MINUTES.toMillis(5L);

    @NonNull
    private final Context context;
    @NonNull
    private final NavigationBatterySnapshotCache cache;
    private final ElapsedRealtimeClock clock;
    private final NavigationBatteryBehaviorDetector behaviorDetector = new NavigationBatteryBehaviorDetector();
    private String diagnostics = "measurements=unavailable";
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
        this(context, elapsedRealtimeClock, true);
    }

    AndroidBatterySnapshotReader(Context context, ElapsedRealtimeClock elapsedRealtimeClock, boolean observeChanges) {
        this.context = observeChanges ? context.getApplicationContext() : context;
        clock = elapsedRealtimeClock;
        cache = new NavigationBatterySnapshotCache(elapsedRealtimeClock, MAXIMUM_SNAPSHOT_AGE_MS);
        if (observeChanges) {
            start();
        }
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
            behaviorDetector.reset();
            cache.record(NavigationBatterySnapshot.unavailable());
        }
    }

    @NonNull
    public NavigationBatterySnapshot read() {
        if (cache.needsRefresh()) {
            record(batteryIntent());
        }
        return cache.current();
    }

    /** Called only by the existing throttled diagnostic sample, never a new timer. */
    public String sampleDiagnostics() {
        record(batteryIntent());
        return diagnostics;
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
            AndroidBatteryReadings readings = AndroidBatteryReadings.read(context, batteryIntent, clock.elapsedRealtimeMs());
            NavigationBatteryBehavior behavior = behaviorDetector.record(readings.observation);
            cache.record(NavigationBatterySnapshot.observed(readings.observation, behavior));
            diagnostics = readings.details + " chargeCounterStatus=" + behavior.reason + " " + behavior.describe();
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to read battery snapshot", e);
            behaviorDetector.reset();
            cache.record(NavigationBatterySnapshot.unavailable());
            diagnostics = "measurements=unavailable error=" + e.getClass().getSimpleName();
        }
    }

    @NonNull
    private static IntentFilter batteryFilter() {
        return new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    }

    static boolean isCharging(@NonNull Intent batteryIntent) {
        int status = batteryIntent.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN
        );
        return batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                || status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
    }

}
