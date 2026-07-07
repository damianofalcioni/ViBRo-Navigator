package vibro.navigator.nav.location;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationMeasurementFormatter;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.model.NavGpsTelemetry;

public final class NavigationGpsTelemetryFormatter {
    private NavigationGpsTelemetryFormatter() {
    }

    @NonNull
    public static NavGpsTelemetry format(
            @NonNull NavigationTextResources resources,
            float speedMps,
            @Nullable NavigationLocation currentLocation,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount
    ) {
        NavGpsTelemetry telemetry = format(
                resources,
                speedMps,
                elevationMeters(currentLocation),
                accuracyMeters,
                bearingDegrees(currentLocation),
                bearingAccuracyDegrees(currentLocation),
                fixedSatelliteCount,
                acquiredFixCount
        );
        return telemetry.withObtainedTimeText(NavigationGpsTextFormatter.formatObtainedTime(
                resources,
                obtainedTimeMs(currentLocation)
        ));
    }

    @NonNull
    public static NavGpsTelemetry format(
            @NonNull NavigationTextResources resources,
            float speedMps,
            @Nullable Double elevationMeters,
            float accuracyMeters,
            @Nullable Float bearingDegrees,
            @Nullable Float bearingAccuracyDegrees,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount
    ) {
        String speedText = NavigationGpsTextFormatter.formatSpeed(resources, speedMps);
        String elevationText = NavigationGpsTextFormatter.formatElevation(resources, elevationMeters);
        String accuracyText = NavigationMeasurementFormatter.formatAccuracyMeters(resources, accuracyMeters);
        String bearingText = NavigationGpsTextFormatter.formatGpsBearing(resources, bearingDegrees);
        String bearingAccuracyText =
                NavigationGpsTextFormatter.formatGpsBearingAccuracy(resources, bearingAccuracyDegrees);
        String satelliteText = NavigationGpsTextFormatter.formatFixedSatelliteCount(resources, fixedSatelliteCount);
        String fixCountText = NavigationGpsTextFormatter.formatAcquiredFixCount(resources, acquiredFixCount);
        String obtainedTimeText = NavigationGpsTextFormatter.formatObtainedTime(resources, null);
        String compactLine = resources.getString(
                R.string.format_nav_gps_status,
                speedText,
                elevationText,
                accuracyText,
                satelliteText
        );
        return new NavGpsTelemetry(
                speedMps,
                compactLine,
                speedText,
                elevationText,
                accuracyText,
                bearingText,
                bearingAccuracyText,
                satelliteText,
                fixCountText,
                obtainedTimeText
        );
    }

    @NonNull
    public static NavGpsTelemetry format(
            @NonNull Context context,
            float speedMps,
            @Nullable Double elevationMeters,
            float accuracyMeters,
            @Nullable Float bearingDegrees,
            @Nullable Float bearingAccuracyDegrees,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount
    ) {
        return format(
                new AndroidNavigationTextResources(context),
                speedMps,
                elevationMeters,
                accuracyMeters,
                bearingDegrees,
                bearingAccuracyDegrees,
                fixedSatelliteCount,
                acquiredFixCount
        );
    }

    @NonNull
    public static String formatDetails(
            @NonNull Context context,
            @NonNull NavGpsTelemetry telemetry,
            @NonNull String intervalText
    ) {
        return context.getString(
                R.string.format_nav_gps_details,
                telemetry.speedText,
                telemetry.elevationText,
                telemetry.accuracyText,
                telemetry.obtainedTimeText,
                telemetry.fixedSatelliteCountText,
                intervalText,
                telemetry.acquiredFixCountText,
                telemetry.bearingText,
                telemetry.bearingAccuracyText
        );
    }

    @NonNull
    public static String formatDetails(
            @NonNull NavigationTextResources resources,
            @NonNull NavGpsTelemetry telemetry,
            @NonNull String intervalText
    ) {
        return resources.getString(
                R.string.format_nav_gps_details,
                telemetry.speedText,
                telemetry.elevationText,
                telemetry.accuracyText,
                telemetry.obtainedTimeText,
                telemetry.fixedSatelliteCountText,
                intervalText,
                telemetry.acquiredFixCountText,
                telemetry.bearingText,
                telemetry.bearingAccuracyText
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
        return currentLocation != null && currentLocation.hasBearingAccuracy()
                ? currentLocation.getBearingAccuracyDegrees()
                : null;
    }

    @Nullable
    private static Long obtainedTimeMs(@Nullable NavigationLocation currentLocation) {
        return currentLocation == null ? null : currentLocation.getTime();
    }
}
