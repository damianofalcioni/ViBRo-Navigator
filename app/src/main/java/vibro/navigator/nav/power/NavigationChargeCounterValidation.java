package vibro.navigator.nav.power;

/** Vendor raw readings remain diagnostic until their scale and discharge direction are plausible. */
public final class NavigationChargeCounterValidation {
    // Generous device-wide bounds, including gauge quantization; these do not validate vendor units.
    private static final double MAX_DISCHARGE_UA = 10_000_000;
    private static final long COUNTER_STEP_ALLOWANCE_UAH = 20_000;
    private NavigationBatterySnapshot previous = NavigationBatterySnapshot.unavailable();

    public String record(int counterUah, boolean charging) {
        if (!charging && !NavigationBatterySnapshot.of(counterUah, -1).hasChargeCounter()) {
            previous = NavigationBatterySnapshot.unavailable();
            return counterUah == Integer.MIN_VALUE ? "unsupported" : "implausible_scale";
        }
        return record(NavigationBatterySnapshot.of(counterUah, -1), charging);
    }

    public String record(NavigationBatterySnapshot snapshot, boolean charging) {
        if (charging) {
            previous = NavigationBatterySnapshot.unavailable();
            return "charging";
        }
        if (!snapshot.hasChargeCounter()) {
            previous = NavigationBatterySnapshot.unavailable();
            return "unsupported";
        }
        String status = rejectionReason(previous, snapshot);
        previous = snapshot;
        return status;
    }

    public static boolean isPlausibleDischarge(NavigationBatterySnapshot previous, NavigationBatterySnapshot current) {
        return current.hasChargeCounter() && "plausible_unvalidated".equals(rejectionReason(previous, current));
    }

    static boolean plausibleCounterChange(double changeUah, long elapsedMs) {
        return elapsedMs >= 0 && changeUah >= 0
                && changeUah <= COUNTER_STEP_ALLOWANCE_UAH + MAX_DISCHARGE_UA * elapsedMs / 3_600_000.0;
    }

    private static String rejectionReason(NavigationBatterySnapshot previous, NavigationBatterySnapshot current) {
        if (!previous.hasChargeCounter()) {
            return "plausible_unvalidated";
        }
        long drop = (long) previous.chargeCounterMicroAmpHours - current.chargeCounterMicroAmpHours;
        if (drop < 0) {
            return "counter_increased";
        }
        return exceedsTimeBound(previous, current, drop) || disagreesWithLevel(previous, current, drop)
                ? "counter_discontinuity" : "plausible_unvalidated";
    }

    private static boolean exceedsTimeBound(NavigationBatterySnapshot previous, NavigationBatterySnapshot current, long drop) {
        if (previous.observedAtElapsedMs < 0 || current.observedAtElapsedMs < 0) {
            // Untimed callers cannot establish the plausibility of a large single-step loss.
            return drop > previous.chargeCounterMicroAmpHours * 0.25;
        }
        long elapsedMs = current.observedAtElapsedMs - previous.observedAtElapsedMs;
        return !plausibleCounterChange(drop, elapsedMs);
    }

    private static boolean disagreesWithLevel(NavigationBatterySnapshot previous, NavigationBatterySnapshot current, long drop) {
        if (!previous.hasLevelPercent() || !current.hasLevelPercent() || previous.levelPercent == 0) {
            return false;
        }
        double expectedPercentDrop = drop * (double) previous.levelPercent / previous.chargeCounterMicroAmpHours;
        return expectedPercentDrop > Math.max(5, previous.levelPercent - current.levelPercent + 5);
    }
}
