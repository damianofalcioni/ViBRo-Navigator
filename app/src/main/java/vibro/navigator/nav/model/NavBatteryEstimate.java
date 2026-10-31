package vibro.navigator.nav.model;

/** A partial measurement has its own explicit coverage, never a reconstructed trip total. */
public final class NavBatteryEstimate {
    public final float milliAmpHours;
    public final boolean wholeTrip;
    public final long coveredFromMs;
    public final long coveredUntilMs;

    private NavBatteryEstimate(float milliAmpHours, boolean wholeTrip, long coveredFromMs, long coveredUntilMs) {
        this.milliAmpHours = Float.isFinite(milliAmpHours) && milliAmpHours >= 0 ? milliAmpHours : Float.NaN;
        this.wholeTrip = wholeTrip;
        this.coveredFromMs = coveredFromMs;
        this.coveredUntilMs = coveredUntilMs;
    }

    public static NavBatteryEstimate wholeTrip(float milliAmpHours) {
        return new NavBatteryEstimate(milliAmpHours, true, -1, -1);
    }

    public static NavBatteryEstimate partial(float milliAmpHours, long coveredFromMs, long coveredUntilMs) {
        return coveredFromMs >= 0 && coveredUntilMs > coveredFromMs
                ? new NavBatteryEstimate(milliAmpHours, false, coveredFromMs, coveredUntilMs) : unavailable();
    }

    public static NavBatteryEstimate unavailable() {
        return new NavBatteryEstimate(Float.NaN, false, -1, -1);
    }

    public long coveredDurationMs() {
        return Math.max(0L, coveredUntilMs - coveredFromMs);
    }
}
