package vibro.navigator.nav.location;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;

import vibro.navigator.R;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationMeasurementFormatter;
import vibro.navigator.nav.format.NavigationTextResources;

public final class NavigationGpsTextFormatter {

    private NavigationGpsTextFormatter() {
    }

    @NonNull
    public static String formatGpsStatus(
            @NonNull Context context,
            float speedMps,
            @Nullable Double elevationMeters,
            float accuracyMeters,
            @Nullable Float bearingDegrees,
            @Nullable Float bearingAccuracyDegrees,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount
    ) {
        return formatGpsStatus(
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
    public static String formatGpsStatus(
            @NonNull NavigationTextResources resources,
            float speedMps,
            @Nullable Double elevationMeters,
            float accuracyMeters,
            @Nullable Float bearingDegrees,
            @Nullable Float bearingAccuracyDegrees,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount
    ) {
        return NavigationGpsTelemetryFormatter.format(
                resources,
                speedMps,
                elevationMeters,
                accuracyMeters,
                bearingDegrees,
                bearingAccuracyDegrees,
                fixedSatelliteCount,
                acquiredFixCount
        ).compactLine;
    }

    @NonNull
    public static String formatSpeed(@NonNull Context context, float speedMps) {
        return NavigationMeasurementFormatter.formatSpeed(context, speedMps);
    }

    @NonNull
    public static String formatSpeed(@NonNull NavigationTextResources resources, float speedMps) {
        return NavigationMeasurementFormatter.formatSpeed(resources, speedMps);
    }

    @NonNull
    public static String formatElevation(@NonNull Context context, @Nullable Double elevationMeters) {
        return NavigationMeasurementFormatter.formatElevation(context, elevationMeters);
    }

    @NonNull
    public static String formatElevation(
            @NonNull NavigationTextResources resources,
            @Nullable Double elevationMeters
    ) {
        return NavigationMeasurementFormatter.formatElevation(resources, elevationMeters);
    }

    @NonNull
    public static String formatAccuracy(@NonNull Context context, float accuracyMeters) {
        return NavigationMeasurementFormatter.formatAccuracy(context, accuracyMeters);
    }

    @NonNull
    public static String formatAccuracy(@NonNull NavigationTextResources resources, float accuracyMeters) {
        return NavigationMeasurementFormatter.formatAccuracy(resources, accuracyMeters);
    }

    @NonNull
    public static String formatFixedSatelliteCount(
            @NonNull Context context,
            @Nullable Integer fixedSatelliteCount
    ) {
        return formatFixedSatelliteCount(
                new AndroidNavigationTextResources(context),
                fixedSatelliteCount
        );
    }

    @NonNull
    public static String formatFixedSatelliteCount(
            @NonNull NavigationTextResources resources,
            @Nullable Integer fixedSatelliteCount
    ) {
        if (fixedSatelliteCount == null || fixedSatelliteCount < 0) {
            return resources.getString(R.string.nav_status_unavailable);
        }
        return resources.getString(R.string.format_nav_satellite_count_value, fixedSatelliteCount);
    }

    @NonNull
    public static String formatAcquiredFixCount(
            @NonNull Context context,
            @Nullable Integer acquiredFixCount
    ) {
        return formatAcquiredFixCount(
                new AndroidNavigationTextResources(context),
                acquiredFixCount
        );
    }

    @NonNull
    public static String formatAcquiredFixCount(
            @NonNull NavigationTextResources resources,
            @Nullable Integer acquiredFixCount
    ) {
        if (acquiredFixCount == null || acquiredFixCount < 0) {
            return resources.getString(R.string.nav_status_unavailable);
        }
        return resources.getString(R.string.format_nav_fix_count_value, acquiredFixCount);
    }

    @NonNull
    public static String formatObtainedTime(
            @NonNull NavigationTextResources resources,
            @Nullable Long obtainedTimeMs
    ) {
        if (obtainedTimeMs == null || obtainedTimeMs <= 0L) {
            return resources.getString(R.string.nav_status_unavailable);
        }
        return resources.getString(R.string.format_nav_gps_obtained_time_value, new Date(obtainedTimeMs));
    }

    @NonNull
    public static String formatGpsBearing(@NonNull Context context, @Nullable Float bearingDegrees) {
        return formatGpsBearing(new AndroidNavigationTextResources(context), bearingDegrees);
    }

    @NonNull
    public static String formatGpsBearing(
            @NonNull NavigationTextResources resources,
            @Nullable Float bearingDegrees
    ) {
        if (bearingDegrees == null || !Float.isFinite(bearingDegrees)) {
            return resources.getString(R.string.nav_status_unavailable);
        }
        return resources.getString(R.string.format_nav_bearing_value, bearingDegrees);
    }

    @NonNull
    public static String formatGpsBearingAccuracy(
            @NonNull Context context,
            @Nullable Float bearingAccuracyDegrees
    ) {
        return formatGpsBearingAccuracy(
                new AndroidNavigationTextResources(context),
                bearingAccuracyDegrees
        );
    }

    @NonNull
    public static String formatGpsBearingAccuracy(
            @NonNull NavigationTextResources resources,
            @Nullable Float bearingAccuracyDegrees
    ) {
        if (bearingAccuracyDegrees == null
                || !Float.isFinite(bearingAccuracyDegrees)
                || bearingAccuracyDegrees < 0f) {
            return resources.getString(R.string.nav_status_unavailable);
        }
        return resources.getString(R.string.format_nav_bearing_accuracy_value, bearingAccuracyDegrees);
    }
}
