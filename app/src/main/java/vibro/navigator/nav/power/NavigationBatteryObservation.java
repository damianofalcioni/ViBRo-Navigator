package vibro.navigator.nav.power;

/** Unscaled device readings. Platform property names do not prove vendor units. */
public final class NavigationBatteryObservation {
    public enum PowerState { DISCHARGING, CHARGING, UNKNOWN }

    public final int counter;
    public final int current;
    public final int levelPercent;
    public final long elapsedMs;
    public final PowerState powerState;

    public NavigationBatteryObservation(int counter, int current, int levelPercent, long elapsedMs,
            PowerState powerState) {
        this.counter = counter;
        this.current = current;
        this.levelPercent = levelPercent;
        this.elapsedMs = elapsedMs;
        this.powerState = powerState;
    }

    public boolean hasCounter() { return counter != Integer.MIN_VALUE; }

    public boolean hasLevel() { return levelPercent >= 0 && levelPercent <= 100; }

    boolean hasCurrent() { return current != Integer.MIN_VALUE && current != 0; }

    boolean levelIncreasedSince(NavigationBatteryObservation earlier) {
        return hasLevel() && earlier.hasLevel() && levelPercent > earlier.levelPercent;
    }

    boolean sameReading(NavigationBatteryObservation other) {
        return counter == other.counter && current == other.current && levelPercent == other.levelPercent
                && elapsedMs == other.elapsedMs && powerState == other.powerState;
    }
}
