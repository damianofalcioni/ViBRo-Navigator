package vibro.navigator.nav.routing;

import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class StartupRouteRefreshPolicy {
    private static final float MIN_START_MOVE_METERS = 10f;
    private static final float MIN_ACCURACY_IMPROVEMENT_METERS = 8f;
    private static final float GPS_REFRESH_ACCURACY_METERS = 12f;

    private StartupRouteRefreshPolicy() {
    }

    public static boolean shouldRefresh(
            @Nullable Location activeRequestStart,
            @Nullable Location latestStart
    ) {
        if (activeRequestStart == null || latestStart == null) {
            return true;
        }
        return hasMovedMaterially(activeRequestStart, latestStart)
                || hasAccuracyImprovedMaterially(activeRequestStart, latestStart)
                || isStrongGpsUpgrade(activeRequestStart, latestStart);
    }

    public static float distanceMeters(
            @Nullable Location activeRequestStart,
            @Nullable Location latestStart
    ) {
        if (activeRequestStart == null || latestStart == null) {
            return Float.NaN;
        }
        return activeRequestStart.distanceTo(latestStart);
    }

    private static boolean hasMovedMaterially(
            @NonNull Location activeRequestStart,
            @NonNull Location latestStart
    ) {
        return activeRequestStart.distanceTo(latestStart) >= MIN_START_MOVE_METERS;
    }

    private static boolean hasAccuracyImprovedMaterially(
            @NonNull Location activeRequestStart,
            @NonNull Location latestStart
    ) {
        return accuracyMeters(activeRequestStart) - accuracyMeters(latestStart)
                >= MIN_ACCURACY_IMPROVEMENT_METERS;
    }

    private static boolean isStrongGpsUpgrade(
            @NonNull Location activeRequestStart,
            @NonNull Location latestStart
    ) {
        return LocationManager.GPS_PROVIDER.equals(latestStart.getProvider())
                && !LocationManager.GPS_PROVIDER.equals(activeRequestStart.getProvider())
                && accuracyMeters(latestStart) <= GPS_REFRESH_ACCURACY_METERS;
    }

    private static float accuracyMeters(@NonNull Location location) {
        return location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
    }
}
