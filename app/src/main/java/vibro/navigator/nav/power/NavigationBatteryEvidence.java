package vibro.navigator.nav.power;

/** Constant-memory, independently spaced evidence within one uninterrupted discharge epoch. */
final class NavigationBatteryEvidence {
    private static final long MIN_SPACING_MS = 30_000L;
    private static final long MIN_SPAN_MS = 180_000L;
    private NavigationBatteryObservation origin;
    private NavigationBatteryObservation previous;
    private int samples;
    private int levelChanges;
    private double currentRawHours;
    private boolean currentUsable;
    private int currentSign;
    private boolean microStepsPlausible;
    private boolean milliStepsPlausible;

    void reset(NavigationBatteryObservation observation) {
        seed(observation);
        microStepsPlausible = true;
        milliStepsPlausible = true;
    }

    private void seed(NavigationBatteryObservation observation) {
        origin = observation;
        previous = observation;
        samples = 1;
        levelChanges = 0;
        currentRawHours = 0;
        currentUsable = observation.hasCurrent();
        currentSign = Integer.signum(observation.current);
    }

    void recordStep(NavigationBatteryObservation earlier, NavigationBatteryObservation observation) {
        microStepsPlausible &= NavigationBatteryInterpretation.plausibleStep(earlier, observation, 1);
        milliStepsPlausible &= NavigationBatteryInterpretation.plausibleStep(earlier, observation, 1_000);
    }

    void record(NavigationBatteryObservation observation) {
        if (!origin.hasLevel() && observation.hasLevel()) {
            seed(observation);
            return;
        }
        long gap = observation.elapsedMs - previous.elapsedMs;
        if (gap < MIN_SPACING_MS) {
            return;
        }
        if (previous.hasLevel() && observation.hasLevel() && observation.levelPercent < previous.levelPercent) {
            levelChanges = Math.min(3, levelChanges + 1);
        }
        recordCurrent(observation, gap);
        previous = observation;
        samples = Math.min(4, samples + 1);
    }

    private void recordCurrent(NavigationBatteryObservation observation, long gap) {
        currentUsable &= observation.hasCurrent() && Integer.signum(observation.current) == currentSign;
        if (currentUsable) {
            currentRawHours += (Math.abs((double) previous.current) + Math.abs((double) observation.current))
                    * 0.5 * gap / 3_600_000.0;
        }
    }

    boolean ready() {
        return samples >= 4 && levelChanges >= 3 && previous.elapsedMs - origin.elapsedMs >= MIN_SPAN_MS
                && origin.hasLevel() && previous.hasLevel() && percentDrop() >= 3;
    }

    int percentDrop() { return origin.levelPercent - previous.levelPercent; }

    long counterChange() { return (long) previous.counter - origin.counter; }

    double currentRawHours() { return currentUsable ? currentRawHours : Double.NaN; }

    int currentSign() { return currentUsable ? currentSign : 0; }

    boolean plausibleSteps(int multiplier) { return multiplier == 1 ? microStepsPlausible : milliStepsPlausible; }
}
