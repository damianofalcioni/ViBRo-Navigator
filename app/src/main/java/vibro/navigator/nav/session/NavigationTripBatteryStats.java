package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import vibro.navigator.nav.model.NavTripStatus;
import vibro.navigator.nav.model.NavBatteryEstimate;
import vibro.navigator.nav.power.NavigationBatterySnapshot;
import vibro.navigator.nav.power.NavigationChargeCounterValidation;

final class NavigationTripBatteryStats {
    private boolean chargeCounterReliable;
    private boolean levelPercentReliable;
    @NonNull
    private NavigationBatterySnapshot initialSnapshot = NavigationBatterySnapshot.unavailable();
    @NonNull
    private NavigationBatterySnapshot latestSnapshot = NavigationBatterySnapshot.unavailable();
    private NavigationBatterySnapshot windowStart = NavigationBatterySnapshot.unavailable();

    void reset() {
        chargeCounterReliable = false;
        levelPercentReliable = false;
        initialSnapshot = NavigationBatterySnapshot.unavailable();
        latestSnapshot = NavigationBatterySnapshot.unavailable();
        windowStart = NavigationBatterySnapshot.unavailable();
    }

    void start(@NonNull NavigationBatterySnapshot snapshot) {
        initialSnapshot = snapshot;
        latestSnapshot = snapshot;
        windowStart = snapshot;
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
        updateMeasurementWindow(snapshot);
        latestSnapshot = snapshot;
    }

    NavBatteryEstimate estimate() {
        float wholeTrip = batteryUsedMilliAmpHours();
        if (Float.isFinite(wholeTrip)) {
            return NavBatteryEstimate.wholeTrip(wholeTrip);
        }
        return NavBatteryEstimate.partial(latestSnapshot.estimatedUsedMilliAmpHours(windowStart),
                windowStart.observedAtElapsedMs, latestSnapshot.observedAtElapsedMs);
    }

    private void updateMeasurementWindow(NavigationBatterySnapshot snapshot) {
        if (!snapshot.hasObservedDischarge() || snapshot.observedAtElapsedMs < latestSnapshot.observedAtElapsedMs) {
            windowStart = NavigationBatterySnapshot.unavailable();
        } else if (!snapshot.sameDischargeEpoch(windowStart)) {
            // Start only at the first observation actually received in the new epoch.
            // Earlier unobserved charge and previous windows cannot become part of it.
            windowStart = snapshot;
        }
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
