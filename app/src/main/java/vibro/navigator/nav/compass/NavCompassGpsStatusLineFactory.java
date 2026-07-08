package vibro.navigator.nav.compass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;

public final class NavCompassGpsStatusLineFactory {
    private NavCompassGpsStatusLineFactory() {
    }

    @NonNull
    public static String build(
            float speedMps,
            @Nullable NavigationLocation currentLocation,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount,
            @NonNull Context context
    ) {
        return build(
                speedMps,
                currentLocation,
                accuracyMeters,
                fixedSatelliteCount,
                acquiredFixCount,
                new AndroidNavigationTextResources(context)
        );
    }

    @NonNull
    public static String build(
            float speedMps,
            @Nullable NavigationLocation currentLocation,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount,
            @NonNull NavigationTextResources textResources
    ) {
        return NavigationTextFormatter.formatGpsStatus(
                textResources,
                speedMps,
                elevationMeters(currentLocation),
                accuracyMeters,
                bearingDegrees(currentLocation),
                bearingAccuracyDegrees(currentLocation),
                fixedSatelliteCount,
                acquiredFixCount
        );
    }

    @Nullable
    private static Double elevationMeters(@Nullable NavigationLocation currentLocation) {
        return currentLocation != null && currentLocation.hasAltitude()
                ? currentLocation.getAltitude()
                : null;
    }

    @Nullable
    private static Float bearingDegrees(@Nullable NavigationLocation currentLocation) {
        return currentLocation != null && currentLocation.hasBearing()
                ? currentLocation.getBearing()
                : null;
    }

    @Nullable
    private static Float bearingAccuracyDegrees(@Nullable NavigationLocation currentLocation) {
        return currentLocation != null
                && currentLocation.hasBearingAccuracy()
                ? currentLocation.getBearingAccuracyDegrees()
                : null;
    }
}
