package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationFormatter;
import vibro.navigator.nav.location.NavigationLocationProviders;

final class NavigationLocationDebugLogger {
    private static final String TAG = "NavSessionLocation";

    private long lastCallbackAtMs = -1L;

    void reset() {
        lastCallbackAtMs = -1L;
    }

    static void droppedNoRecentCandidate(@NonNull NavigationLocation rawLocation, @Nullable String activeProviders) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        String reason = NavigationLocationProviders.isActive(activeProviders, rawLocation.getProvider())
                ? "no_recent_valid_candidate" : "inactive_provider";
        AppLogger.d(TAG, "Dropped NavigationLocation reason=" + reason + " raw="
                + NavigationLocationFormatter.format(rawLocation));
    }

    void incoming(@NonNull NavigationLocation location, long nowMs, long intervalMs, long acceptedGapMs) {
        long callbackGapMs = lastCallbackAtMs < 0 ? -1L : nowMs - lastCallbackAtMs;
        lastCallbackAtMs = nowMs;
        if (AppLogger.isLoggingEnabled()) {
            AppLogger.d(TAG, "Location callback incoming=" + NavigationLocationFormatter.format(location)
                    + " incomingStatus=" + NavigationLocationFormatter.incomingStatus(location, nowMs)
                    + " incomingAgeMs=" + (nowMs - location.getElapsedRealtimeOrTimeMs())
                    + " nowElapsedMs=" + nowMs + " requestedIntervalMs=" + intervalMs
                    + " actualCallbackGapMs=" + callbackGapMs + " acceptedFixGapMs=" + acceptedGapMs);
        }
    }

    static void dropped(
            @NonNull NavigationLocation rawLocation,
            @NonNull NavigationLocation selected,
            long nowMs,
            @NonNull String reason,
            boolean positionCorrected
    ) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        AppLogger.d(TAG, "Dropped NavigationLocation selectedDispatchReason=" + reason + " incoming="
                + NavigationLocationFormatter.format(rawLocation)
                + " selected=" + NavigationLocationFormatter.format(selected)
                + " incomingAgeMs=" + (nowMs - rawLocation.getElapsedRealtimeOrTimeMs())
                + " selectedAgeMs=" + (nowMs - selected.getElapsedRealtimeOrTimeMs())
                + " incomingSelectionReason=" + selectionReason(rawLocation, selected, nowMs)
                + " delayedPositionCorrection=" + positionCorrected);
    }

    static String selectionReason(NavigationLocation incoming, NavigationLocation selected, long nowMs) {
        if (nowMs - incoming.getElapsedRealtimeOrTimeMs() > 15_000L) {
            return "stale_incoming";
        }
        if (Objects.equals(incoming.getProvider(), selected.getProvider())
                && incoming.getElapsedRealtimeOrTimeMs() == selected.getElapsedRealtimeOrTimeMs()) {
            return "incoming_selected_dispatch_blocked";
        }
        if (!NavigationLocationProviders.NETWORK_PROVIDER.equals(incoming.getProvider())) {
            return "retained_other_measurement";
        }
        if (incoming.getElapsedRealtimeOrTimeMs() + 1_000L < selected.getElapsedRealtimeOrTimeMs()) {
            return "network_older_than_timing_tolerance";
        }
        return incoming.getAccuracy() < selected.getAccuracy()
                ? "accuracy_improvement_below_switch_margin" : "selected_accuracy_or_provider_hysteresis";
    }

    static void reacquiringAfterLongGap(@NonNull NavigationLocation selected, long gapMs) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        AppLogger.i(TAG, "Reacquiring NavigationLocation after long accepted-fix gap raw="
                + NavigationLocationFormatter.format(selected)
                + " gapMs=" + gapMs);
    }

    static void resettingStartupFilter(@NonNull NavigationLocation selected) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        AppLogger.i(TAG, "Resetting startup NavigationLocation filter after route-grade fix raw="
                + NavigationLocationFormatter.format(selected));
    }

    static void kalmanDropped(@NonNull NavigationLocation selected, @Nullable String reason) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        AppLogger.d(TAG, "Kalman filter dropped NavigationLocation reason=" + reason
                + " raw=" + NavigationLocationFormatter.format(selected));
    }

    static void accepted(
            int locationUpdateCount,
            @NonNull NavigationLocation selected,
            @NonNull NavigationLocation filtered,
            boolean stationary,
            float motionSpeedMps,
            boolean departureChecks
    ) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        AppLogger.d(TAG, "NavigationLocation update #" + locationUpdateCount
                + " raw=" + NavigationLocationFormatter.format(selected)
                + " filtered=" + NavigationLocationFormatter.format(filtered)
                + " smoothingShiftMeters=" + selected.distanceTo(filtered)
                + " stationary=" + stationary + " motionSpeed=" + motionSpeedMps
                + " departureChecks=" + departureChecks);
    }
}
