package vibro.navigator.nav.power;

import androidx.annotation.NonNull;

public final class NavigationBatterySnapshot {
    private static final int UNKNOWN = Integer.MIN_VALUE;
    // A phone reporting positive state of charge cannot plausibly have only a few hundred microamp-hours.
    private static final int MIN_CHARGE_COUNTER_UAH = 1_000;

    public final int chargeCounterMicroAmpHours;
    public final int levelPercent;
    public final long observedAtElapsedMs;

    private NavigationBatterySnapshot(int chargeCounterMicroAmpHours, int levelPercent, long observedAtElapsedMs) {
        this.chargeCounterMicroAmpHours = cleanChargeCounter(chargeCounterMicroAmpHours);
        this.levelPercent = cleanLevelPercent(levelPercent);
        this.observedAtElapsedMs = observedAtElapsedMs;
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
