package vibro.navigator.nav.guidance;

import androidx.annotation.NonNull;

public final class NavigationDeviationConfirmation {

    private static final int DEVIATION_CONFIRMATION_SAMPLES = 2;
    private static final long MINIMUM_CONFIRMATION_SPACING_MS = 750L;

    @NonNull
    private RouteDeviationPolicy.Reason pendingDeviationReason = RouteDeviationPolicy.Reason.NONE;
    private int pendingDeviationSampleCount;
    private long lastDeviationSampleMs;

    public boolean isConfirmed(@NonNull RouteDeviationPolicy.Decision decision, long nowMs) {
        if (pendingDeviationReason != decision.reason) {
            pendingDeviationReason = decision.reason;
            pendingDeviationSampleCount = 1;
            lastDeviationSampleMs = nowMs;
            return false;
        }
        if (nowMs - lastDeviationSampleMs < MINIMUM_CONFIRMATION_SPACING_MS) {
            return false;
        }
        lastDeviationSampleMs = nowMs;
        pendingDeviationSampleCount++;
        return pendingDeviationSampleCount >= DEVIATION_CONFIRMATION_SAMPLES;
    }

    public int pendingSampleCount() {
        return pendingDeviationSampleCount;
    }

    public void clear() {
        pendingDeviationReason = RouteDeviationPolicy.Reason.NONE;
        pendingDeviationSampleCount = 0;
        lastDeviationSampleMs = 0L;
    }
}
