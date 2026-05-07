package vibro.navigator.nav.location;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.format.NavigationMeasurementFormatter;

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
    public static String formatSpeed(@NonNull Context context, float speedMps) {
        return NavigationMeasurementFormatter.formatSpeed(context, speedMps);
    }

    @NonNull
    public static String formatElevation(@NonNull Context context, @Nullable Double elevationMeters) {
        return NavigationMeasurementFormatter.formatElevation(context, elevationMeters);
    }

    @NonNull
    public static String formatAccuracy(@NonNull Context context, float accuracyMeters) {
        return NavigationMeasurementFormatter.formatAccuracy(context, accuracyMeters);
    }

    @NonNull
    public static String formatFixedSatelliteCount(
            @NonNull Context context,
            @Nullable Integer fixedSatelliteCount
    ) {
        if (fixedSatelliteCount == null || fixedSatelliteCount < 0) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_satellite_count_value, fixedSatelliteCount);
    }

    @NonNull
    public static String formatGpsBearing(@NonNull Context context, @Nullable Float bearingDegrees) {
        if (bearingDegrees == null || !Float.isFinite(bearingDegrees)) {
            return context.getString(R.string.nav_status_unavailable);
        }
        return context.getString(R.string.format_nav_bearing_value, bearingDegrees);
    }

    @NonNull
    public static String formatGpsBearingAccuracy(
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
