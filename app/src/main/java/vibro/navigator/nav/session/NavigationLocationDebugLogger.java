package vibro.navigator.nav.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationFormatter;

final class NavigationLocationDebugLogger {
    private static final String TAG = "NavSessionLocation";

    private NavigationLocationDebugLogger() {
    }

    static void droppedNoRecentCandidate(@NonNull NavigationLocation rawLocation) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        AppLogger.d(TAG, "Dropped NavigationLocation because no recent candidate is available raw="
                + NavigationLocationFormatter.format(rawLocation));
    }

    static void droppedUnchanged(
            @NonNull NavigationLocation rawLocation,
            @NonNull NavigationLocation selected
    ) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        AppLogger.d(TAG, "Dropped NavigationLocation because selected candidate is unchanged raw="
                + NavigationLocationFormatter.format(rawLocation)
                + " selected=" + NavigationLocationFormatter.format(selected));
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
            float motionSpeedMps
    ) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        AppLogger.d(TAG, "NavigationLocation update #" + locationUpdateCount
                + " raw=" + NavigationLocationFormatter.format(selected)
                + " filtered=" + NavigationLocationFormatter.format(filtered)
                + " smoothingShiftMeters=" + selected.distanceTo(filtered)
                + " stationary=" + stationary + " motionSpeed=" + motionSpeedMps);
    }
}
