package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import vibro.navigator.nav.model.NavTripStatus;
import vibro.navigator.nav.power.NavigationBatterySnapshot;

final class NavigationTripBatteryStats {
    private boolean chargeCounterReliable;
    private boolean levelPercentReliable;
    @NonNull
    private NavigationBatterySnapshot initialSnapshot = NavigationBatterySnapshot.unavailable();
    @NonNull
    private NavigationBatterySnapshot latestSnapshot = NavigationBatterySnapshot.unavailable();

    void reset() {
        chargeCounterReliable = false;
        levelPercentReliable = false;
        initialSnapshot = NavigationBatterySnapshot.unavailable();
        latestSnapshot = NavigationBatterySnapshot.unavailable();
    }

    void start(@NonNull NavigationBatterySnapshot snapshot) {
        initialSnapshot = snapshot;
        latestSnapshot = snapshot;
        chargeCounterReliable = snapshot.hasChargeCounter();
        levelPercentReliable = snapshot.hasLevelPercent();
    }

    void recordSnapshot(@NonNull NavigationBatterySnapshot snapshot) {
        if (latestSnapshot.hasSameReading(snapshot)) {
            return;
        }
        chargeCounterReliable = chargeCounterReliable && snapshot.hasChargeCounter();
        levelPercentReliable = levelPercentReliable && snapshot.hasLevelPercent();
        latestSnapshot = snapshot;
    }

    float batteryUsedMilliAmpHours() {
        if (!chargeCounterReliable || !initialSnapshot.hasChargeCounter()
                || !latestSnapshot.hasChargeCounter()) {
            return Float.NaN;
        }
        long usedMicroAmpHours = (long) initialSnapshot.chargeCounterMicroAmpHours
                - latestSnapshot.chargeCounterMicroAmpHours;
        return usedMicroAmpHours >= 0L ? usedMicroAmpHours / 1000f : Float.NaN;
    }

    int batteryDropPercent() {
        if (!levelPercentReliable || !initialSnapshot.hasLevelPercent()
                || !latestSnapshot.hasLevelPercent()) {
            return NavTripStatus.UNKNOWN_BATTERY_DROP_PERCENT;
        }
        int drop = initialSnapshot.levelPercent - latestSnapshot.levelPercent;
        return drop >= 0 ? drop : NavTripStatus.UNKNOWN_BATTERY_DROP_PERCENT;
    }
}
