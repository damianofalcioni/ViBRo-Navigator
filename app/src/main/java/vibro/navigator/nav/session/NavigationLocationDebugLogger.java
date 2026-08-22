package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.location.NavigationLocation;

final class NavigationLocationDebugLogger {
    private static final String TAG = "NavSessionLocation";

    private NavigationLocationDebugLogger() {
    }

    static void droppedNoRecentCandidate(@NonNull NavigationLocation rawLocation) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        AppLogger.d(TAG, "Dropped NavigationLocation because no recent candidate is available raw="
                + formatLocation(rawLocation));
    }

    static void droppedUnchanged(
            @NonNull NavigationLocation rawLocation,
            @NonNull NavigationLocation selected
    ) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        AppLogger.d(TAG, "Dropped NavigationLocation because selected candidate is unchanged raw="
                + formatLocation(rawLocation)
                + " selected=" + formatLocation(selected));
    }

    static void reacquiringAfterLongGap(@NonNull NavigationLocation selected, long gapMs) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        AppLogger.i(TAG, "Reacquiring NavigationLocation after long accepted-fix gap raw="
                + formatLocation(selected)
                + " gapMs=" + gapMs);
    }

    static void resettingStartupFilter(@NonNull NavigationLocation selected) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        AppLogger.i(TAG, "Resetting startup NavigationLocation filter after route-grade fix raw="
                + formatLocation(selected));
    }

    static void kalmanDropped(@NonNull NavigationLocation selected) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        AppLogger.d(TAG, "Kalman filter dropped NavigationLocation " + formatLocation(selected));
    }

    static void accepted(
            int locationUpdateCount,
            @NonNull NavigationLocation selected,
            @NonNull NavigationLocation filtered
    ) {
        if (!AppLogger.isLoggingEnabled()) {
            return;
        }
        AppLogger.d(TAG, "NavigationLocation update #" + locationUpdateCount
                + " raw=" + formatLocation(selected)
                + " filtered=" + formatLocation(filtered));
    }

    @NonNull
    private static String formatLocation(@NonNull NavigationLocation location) {
        StringBuilder text = new StringBuilder(96);
        text.append(location.getProvider())
                .append("(")
                .append(location.getLatitude())
                .append(",")
                .append(location.getLongitude())
                .append(")");
        if (location.hasAccuracy()) {
            text.append(" acc=").append(location.getAccuracy());
        }
        if (location.hasSpeed()) {
            text.append(" speed=").append(location.getSpeed());
        }
        if (location.hasBearing()) {
            text.append(" bearing=").append(location.getBearing());
        }
        text.append(" time=").append(location.getTime());
        return text.toString();
    }
}
