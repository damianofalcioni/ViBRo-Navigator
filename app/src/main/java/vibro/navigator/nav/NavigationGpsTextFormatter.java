package vibro.navigator.nav;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;

final class NavigationGpsTextFormatter {

    private NavigationGpsTextFormatter() {
    }

    @NonNull
    static String formatGpsStatus(
            @NonNull Context context,
            float speedMps,
            @Nullable Double elevationMeters,
            float accuracyMeters,
            @Nullable Float bearingDegrees,
            @Nullable Float bearingAccuracyDegrees,
            @Nullable Integer fixedSatelliteCount
    ) {
        return context.getString(
                R.string.format_nav_gps_status,
                formatSpeed(context, speedMps),
                formatElevation(context, elevationMeters),
                formatGpsBearing(context, bearingDegrees),
                formatAccuracy(context, accuracyMeters),
                formatGpsBearingAccuracy(context, bearingAccuracyDegrees),
                formatFixedSatelliteCount(context, fixedSatelliteCount)
        );
    }

    @NonNull
    static String formatSpeed(@NonNull Context context, float speedMps) {
        if (!Float.isFinite(speedMps) || speedMps < 0f) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_speed_value, speedMps * 3.6f);
    }

    @NonNull
    static String formatElevation(@NonNull Context context, @Nullable Double elevationMeters) {
        if (elevationMeters == null || !Double.isFinite(elevationMeters)) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_elevation_value, elevationMeters);
    }

    @NonNull
    static String formatAccuracy(@NonNull Context context, float accuracyMeters) {
        if (!Float.isFinite(accuracyMeters) || accuracyMeters <= 0f) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_accuracy_value, accuracyMeters);
    }

    @NonNull
    static String formatFixedSatelliteCount(
            @NonNull Context context,
            @Nullable Integer fixedSatelliteCount
    ) {
        if (fixedSatelliteCount == null || fixedSatelliteCount < 0) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_satellite_count_value, fixedSatelliteCount);
    }

    @NonNull
    static String formatGpsBearing(@NonNull Context context, @Nullable Float bearingDegrees) {
        if (bearingDegrees == null || !Float.isFinite(bearingDegrees)) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_bearing_value, bearingDegrees);
    }

    @NonNull
    static String formatGpsBearingAccuracy(
            @NonNull Context context,
            @Nullable Float bearingAccuracyDegrees
    ) {
        if (bearingAccuracyDegrees == null
                || !Float.isFinite(bearingAccuracyDegrees)
                || bearingAccuracyDegrees < 0f) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_bearing_accuracy_value, bearingAccuracyDegrees);
    }
}
