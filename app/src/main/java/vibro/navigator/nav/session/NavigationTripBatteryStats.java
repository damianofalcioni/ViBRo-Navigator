package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import vibro.navigator.nav.model.NavTripStatus;
import vibro.navigator.nav.power.NavigationBatterySnapshot;
import vibro.navigator.nav.power.NavigationChargeCounterValidation;

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
        chargeCounterReliable = snapshot.hasChargeCounter() || snapshot.hasObservedDischarge();
        levelPercentReliable = snapshot.hasLevelPercent();
    }

    void recordSnapshot(@NonNull NavigationBatterySnapshot snapshot) {
        if (snapshot == latestSnapshot) {
            return;
        }
        chargeCounterReliable = chargeCounterReliable
                && counterContinues(snapshot);
        levelPercentReliable = levelPercentReliable && snapshot.hasLevelPercent()
                && snapshot.levelPercent <= latestSnapshot.levelPercent;
        latestSnapshot = snapshot;
    }

    float batteryUsedMilliAmpHours() {
        if (chargeCounterReliable && initialSnapshot.observation != null) {
            return latestSnapshot.estimatedUsedMilliAmpHours(initialSnapshot);
        }
        if (!chargeCounterReliable || !initialSnapshot.hasChargeCounter()
                || !latestSnapshot.hasChargeCounter()) {
            return Float.NaN;
        }
        long usedMicroAmpHours = (long) initialSnapshot.chargeCounterMicroAmpHours
                - latestSnapshot.chargeCounterMicroAmpHours;
        return usedMicroAmpHours >= 0L ? usedMicroAmpHours / 1000f : Float.NaN;
    }

    private boolean counterContinues(NavigationBatterySnapshot snapshot) {
        if (initialSnapshot.observation != null) {
            return snapshot.sameDischargeEpoch(initialSnapshot)
                    && snapshot.observedAtElapsedMs >= latestSnapshot.observedAtElapsedMs;
        }
        return snapshot.observation == null
                && NavigationChargeCounterValidation.isPlausibleDischarge(latestSnapshot, snapshot);
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
