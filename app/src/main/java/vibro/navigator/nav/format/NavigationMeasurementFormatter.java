package vibro.navigator.nav.format;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.settings.AppSettings;

public final class NavigationMeasurementFormatter {
    private static final double METERS_PER_MILE = 1609.344;
    private static final double METERS_PER_FOOT = 0.3048;
    private static final double METERS_PER_SECOND_TO_MILES_PER_HOUR = 2.2369362921;
    private static final double MIN_MILES_DISPLAY_DISTANCE = 0.1;

    private NavigationMeasurementFormatter() {
    }

    @NonNull
    public static String formatDistance(@NonNull Context context, double meters) {
        if (AppSettings.isImperialUnitsEnabled(context)) {
            return formatImperialDistance(context, meters);
        }
        return formatMetricDistance(context, meters);
    }

    @NonNull
    public static String formatSpeed(@NonNull Context context, float speedMps) {
        if (!Float.isFinite(speedMps) || speedMps < 0f) {
            return context.getString(R.string.nav_status_unavailable);
        }
        if (AppSettings.isImperialUnitsEnabled(context)) {
            return context.getString(R.string.format_nav_speed_mph_value,
                    speedMps * METERS_PER_SECOND_TO_MILES_PER_HOUR);
        }
        return context.getString(R.string.format_nav_speed_value, speedMps * 3.6f);
    }

    @NonNull
    public static String formatElevation(@NonNull Context context, @Nullable Double elevationMeters) {
        if (elevationMeters == null || !Double.isFinite(elevationMeters)) {
            return context.getString(R.string.nav_status_unavailable);
        }
        if (AppSettings.isImperialUnitsEnabled(context)) {
            return context.getString(R.string.format_nav_elevation_ft_value, elevationMeters / METERS_PER_FOOT);
        }
        return context.getString(R.string.format_nav_elevation_value, elevationMeters);
    }

    @NonNull
    public static String formatAccuracy(@NonNull Context context, float accuracyMeters) {
        if (!Float.isFinite(accuracyMeters) || accuracyMeters <= 0f) {
            return context.getString(R.string.nav_status_unavailable);
        }
        if (AppSettings.isImperialUnitsEnabled(context)) {
            return context.getString(R.string.format_nav_accuracy_ft_value, accuracyMeters / METERS_PER_FOOT);
        }
        return context.getString(R.string.format_nav_accuracy_value, accuracyMeters);
    }

    @NonNull
    private static String formatMetricDistance(@NonNull Context context, double meters) {
        if (meters >= 1000.0) {
            return context.getString(R.string.format_distance_km, meters / 1000.0);
        }
        return context.getString(R.string.format_distance_m, meters);
    }

    @NonNull
    private static String formatImperialDistance(@NonNull Context context, double meters) {
        double miles = meters / METERS_PER_MILE;
        if (miles >= MIN_MILES_DISPLAY_DISTANCE) {
            return context.getString(R.string.format_distance_mi, miles);
        }
        return context.getString(R.string.format_distance_ft, meters / METERS_PER_FOOT);
    }
}
