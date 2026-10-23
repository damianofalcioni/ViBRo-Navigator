package vibro.navigator.nav.location;

import vibro.navigator.logging.AppLogger;

/** Conservative stop confirmation, retaining a fixed origin across periodic check bursts. */
public final class NavigationStationarySleep {
    public static final long CHECK_INTERVAL_MS = 30_000L;
    private static final long CONFIRM_STOP_MS = 20_000L;
    private final NavigationMotionTrigger trigger;
    private final Runnable onMotion;
    private final StationaryFixCadence cadence = new StationaryFixCadence();
    private final StationarySleepDiagnostics diagnostics;
    private NavigationLocation origin;
    private long firstFixMs = -1L;
    private long lastFixMs = -1L;
    private long lastMeasurementMs = -1L;
    private long deadlineMs = -1L;
    private int fixes;
    private boolean armed;
    private boolean checking;

    public NavigationStationarySleep(NavigationMotionTrigger trigger, Runnable onMotion) {
        this(trigger, onMotion, message -> AppLogger.d("NavStationarySleep", message));
    }

    NavigationStationarySleep(NavigationMotionTrigger trigger, Runnable onMotion,
            StationarySleepDiagnostics.Sink diagnosticSink) {
        this.trigger = trigger;
        this.onMotion = onMotion;
        diagnostics = new StationarySleepDiagnostics(diagnosticSink);
    }

    public boolean observe(NavigationLocation raw, NavigationLocation filtered, boolean eligible, long nowMs) {
        String rejection = eligible ? StationaryFixEligibility.rejection(raw, filtered, nowMs) : "not_eligible";
        if (rejection != null) {
            report(rejection, raw, nowMs);
            reset();
            return false;
        }
        long measurementMs = raw.getElapsedRealtimeMs();
        if (!independent(measurementMs, nowMs)) {
            return false;
        }
        prepareConfirmation(raw, nowMs);
        if (!ensureArmed()) {
            report("motion_trigger_unavailable_or_registration_failed", raw, nowMs);
            return false;
        }
        if (firstFixMs < 0L) {
            firstFixMs = nowMs;
        }
        fixes++;
        boolean ready = confirmed(nowMs);
        report(ready ? "ready" : "confirming_with_armed_motion_trigger", raw, nowMs);
        lastFixMs = nowMs;
        lastMeasurementMs = measurementMs;
        return ready;
    }

    private boolean confirmed(long nowMs) {
        long confirmationMs = checking ? 4_000L : CONFIRM_STOP_MS;
        return fixes >= 3 && nowMs - firstFixMs >= confirmationMs;
    }

    private boolean independent(long measurementMs, long nowMs) {
        return measurementMs > lastMeasurementMs && (lastFixMs < 0 || nowMs - lastFixMs >= 2_000L);
    }

    private void prepareConfirmation(NavigationLocation raw, long nowMs) {
        String reason = restartReason(raw, nowMs);
        if (reason != null) {
            if (origin != null) {
                report(reason, raw, nowMs);
            }
            resetConfirmation();
            origin = new NavigationLocation(raw);
        }
        cadence.record(nowMs);
    }

    private String restartReason(NavigationLocation raw, long nowMs) {
        if (origin == null) {
            return "new_stop_window";
        }
        if (!(origin.distanceTo(raw) <= 3f)) {
            return "position_left_stop_origin";
        }
        return lastFixMs >= 0 && nowMs - lastFixMs > cadence.gapLimitMs() ? "callback_gap" : null;
    }

    private void report(String reason, NavigationLocation raw, long nowMs) {
        diagnostics.record(reason, nowMs, raw, firstFixMs < 0L ? 0L : nowMs - firstFixMs,
                lastFixMs < 0L ? -1L : nowMs - lastFixMs, cadence.gapLimitMs());
    }

    private boolean ensureArmed() {
        if (!armed) {
            armed = trigger.arm(this::motionDetected);
        }
        if (!armed) {
            reset();
        }
        return armed;
    }

    public void beginSleep(long nowMs) {
        deadlineMs = nowMs + CHECK_INTERVAL_MS;
    }

    public boolean isSleeping() {
        return deadlineMs >= 0L;
    }

    public long deadlineMs() {
        return deadlineMs;
    }

    public void beginCheck() {
        cadence.beginBurst();
        deadlineMs = -1L;
        checking = true;
        firstFixMs = -1L;
        lastFixMs = -1L;
        fixes = 0;
    }

    public void reset() {
        cadence.reset();
        resetConfirmation();
    }

    private void resetConfirmation() {
        trigger.cancel();
        armed = false;
        origin = null;
        firstFixMs = -1L;
        lastFixMs = -1L;
        lastMeasurementMs = -1L;
        deadlineMs = -1L;
        checking = false;
        fixes = 0;
    }

    private void motionDetected() {
        reset();
        onMotion.run();
    }
}
