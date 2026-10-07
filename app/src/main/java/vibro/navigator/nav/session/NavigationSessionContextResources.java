package vibro.navigator.nav.session;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.time.NavigationDisplayTime;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.settings.AppCompassSettings;
import vibro.navigator.settings.AppNotificationSettings;

public final class NavigationSessionContextResources {
    private static final String TAG = "NavigationSession";

    private NavigationSessionContextResources() {
    }

    @NonNull
    public static NavState buildState(
            @NonNull NavigationSession session,
            @NonNull Context context,
            long nextEvaluationDeadlineElapsedMs,
            @NonNull NavigationDisplayTime time,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double displayHeadingDegrees,
            @Nullable Float displayHeadingAccuracyDegrees,
            @Nullable CompassOrientationCue orientationCue
    ) {
        return NavigationSessionResourceAdapter.buildState(
                session,
                textResources(session, context),
                nextEvaluationDeadlineElapsedMs,
                time,
                fixedSatelliteCount,
                displayHeadingDegrees,
                displayHeadingAccuracyDegrees,
                orientationCue,
                !isInstantZoomEnabled(context),
                isStationaryFullRouteZoomEnabled(context)
        );
    }

    @NonNull
    static NavigationTextResources textResources(
            @NonNull NavigationSession session,
            @NonNull Context context
    ) {
        if (session.textResources == null || context != session.textResourcesContext) {
            session.textResourcesContext = context;
            session.textResources = new AndroidNavigationTextResources(context);
        }
        return session.textResources;
    }

    static void logRequestLoaded(@NonNull NavigationRequest request) {
        AppLogger.i(TAG, "Navigation request loaded " + request.describe());
    }

    static long defaultLocationUpdateIntervalMs() {
        return NavigationLocationController.DEFAULT_UPDATE_INTERVAL_MS;
    }

    static boolean isInstantZoomEnabled(@NonNull Context context) {
        return AppCompassSettings.isInstantZoomEnabled(context);
    }

    static boolean isStationaryFullRouteZoomEnabled(@NonNull Context context) {
        return AppCompassSettings.isStationaryFullRouteZoomEnabled(context);
    }

    static boolean isSingleInstructionModeEnabled(@NonNull Context context) {
        return AppNotificationSettings.isSingleInstructionModeEnabled(context);
    }
}
