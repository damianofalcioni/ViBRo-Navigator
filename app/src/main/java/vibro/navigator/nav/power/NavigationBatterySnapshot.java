package vibro.navigator.nav.power;

import androidx.annotation.NonNull;

public final class NavigationBatterySnapshot {
    private static final int UNKNOWN = Integer.MIN_VALUE;

    public final int chargeCounterMicroAmpHours;
    public final int levelPercent;

    private NavigationBatterySnapshot(int chargeCounterMicroAmpHours, int levelPercent) {
        this.chargeCounterMicroAmpHours = cleanChargeCounter(chargeCounterMicroAmpHours);
        this.levelPercent = cleanLevelPercent(levelPercent);
    }

    @NonNull
    public static NavigationBatterySnapshot of(int chargeCounterMicroAmpHours, int levelPercent) {
        return new NavigationBatterySnapshot(chargeCounterMicroAmpHours, levelPercent);
    }

    @NonNull
    public static NavigationBatterySnapshot unavailable() {
        return new NavigationBatterySnapshot(UNKNOWN, UNKNOWN);
    }

    public boolean hasChargeCounter() {
        return chargeCounterMicroAmpHours > 0;
    }

    public boolean hasLevelPercent() {
        return levelPercent >= 0 && levelPercent <= 100;
    }
    public boolean hasSameReading(@NonNull NavigationBatterySnapshot other) {
        return chargeCounterMicroAmpHours == other.chargeCounterMicroAmpHours
                && levelPercent == other.levelPercent;
    }

    private static int cleanChargeCounter(int value) {
        return value > 0 ? value : UNKNOWN;
    }

    private static int cleanLevelPercent(int value) {
        return value >= 0 && value <= 100 ? value : UNKNOWN;
    }
}
