package vibro.navigator.nav.power;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationBatterySnapshot {
    private static final int UNKNOWN = Integer.MIN_VALUE;
    // A phone reporting positive state of charge cannot plausibly have only a few hundred microamp-hours.
    private static final int MIN_CHARGE_COUNTER_UAH = 1_000;

    public final int chargeCounterMicroAmpHours;
    public final int levelPercent;
    public final long observedAtElapsedMs;
    @Nullable public final NavigationBatteryObservation observation;
    @Nullable public final NavigationBatteryBehavior behavior;

    private NavigationBatterySnapshot(int chargeCounterMicroAmpHours, int levelPercent, long observedAtElapsedMs) {
        this(chargeCounterMicroAmpHours, levelPercent, observedAtElapsedMs, null, null);
    }

    private NavigationBatterySnapshot(int counter, int levelPercent, long observedAtElapsedMs,
            @Nullable NavigationBatteryObservation observation, @Nullable NavigationBatteryBehavior behavior) {
        this.chargeCounterMicroAmpHours = cleanChargeCounter(counter);
        this.levelPercent = cleanLevelPercent(levelPercent);
        this.observedAtElapsedMs = observedAtElapsedMs;
        this.observation = observation;
        this.behavior = behavior;
    }

    @NonNull
    public static NavigationBatterySnapshot of(int chargeCounterMicroAmpHours, int levelPercent) {
        return of(chargeCounterMicroAmpHours, levelPercent, -1L);
    }

    @NonNull
    public static NavigationBatterySnapshot of(int chargeCounterMicroAmpHours, int levelPercent, long observedAtElapsedMs) {
        return new NavigationBatterySnapshot(chargeCounterMicroAmpHours, levelPercent, observedAtElapsedMs);
    }

    @NonNull
    public static NavigationBatterySnapshot unavailable() {
        return new NavigationBatterySnapshot(UNKNOWN, UNKNOWN, -1L);
    }

    public static NavigationBatterySnapshot observed(NavigationBatteryObservation observation,
            NavigationBatteryBehavior behavior) {
        int level = observation.powerState == NavigationBatteryObservation.PowerState.DISCHARGING
                ? observation.levelPercent : UNKNOWN;
        return new NavigationBatterySnapshot(UNKNOWN, level, observation.elapsedMs, observation, behavior);
    }

    public boolean hasObservedDischarge() {
        return observation != null && observation.hasCounter()
                && observation.elapsedMs >= 0 && behavior != null
                && behavior.state != NavigationBatteryBehavior.State.UNAVAILABLE
                && observation.powerState == NavigationBatteryObservation.PowerState.DISCHARGING;
    }

    public boolean sameDischargeEpoch(NavigationBatterySnapshot initial) {
        return hasObservedDischarge() && initial.hasObservedDischarge() && behavior != null
                && initial.behavior != null && behavior.epoch == initial.behavior.epoch
                && observedAtElapsedMs >= initial.observedAtElapsedMs;
    }

    public float estimatedUsedMilliAmpHours(NavigationBatterySnapshot initial) {
        if (!sameDischargeEpoch(initial) || behavior == null || !behavior.isInferred()) {
            return Float.NaN;
        }
        // sameDischargeEpoch establishes both observations. Keep the signed raw difference
        // before scaling; signed and increasing counters need not represent remaining charge.
        double used = ((double) observation.counter - initial.observation.counter)
                * behavior.dischargeDirection * behavior.counterMultiplier / 1_000.0;
        return used >= 0 ? (float) used : Float.NaN;
    }

    public boolean hasChargeCounter() {
        return chargeCounterMicroAmpHours > 0;
    }

    public boolean hasLevelPercent() {
        return levelPercent >= 0 && levelPercent <= 100;
    }
    private static int cleanChargeCounter(int value) {
        return value >= MIN_CHARGE_COUNTER_UAH ? value : UNKNOWN;
    }

    private static int cleanLevelPercent(int value) {
        return value >= 0 && value <= 100 ? value : UNKNOWN;
    }
}
