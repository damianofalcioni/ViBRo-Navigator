package vibro.navigator.nav.format;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;

public final class NavigationMeasurementFormatter {
    private static final double METERS_PER_MILE = 1609.344;
    private static final double METERS_PER_FOOT = 0.3048;
    private static final double METERS_PER_SECOND_TO_MILES_PER_HOUR = 2.2369362921;
    private static final double MIN_MILES_DISPLAY_DISTANCE = 0.1;
    private static final float MAX_DISPLAY_ACCURACY_METERS = 40_100_000f;

    private NavigationMeasurementFormatter() {
    }

    @NonNull
    public static String formatDistance(@NonNull Context context, double meters) {
        return formatDistance(new AndroidNavigationTextResources(context), meters);
    }

    @NonNull
    public static String formatDistance(@NonNull NavigationTextResources resources, double meters) {
        if (resources.isImperialUnitsEnabled()) {
            return formatImperialDistance(resources, meters);
        }
        return formatMetricDistance(resources, meters);
    }

    @NonNull
    public static String formatSpeed(@NonNull Context context, float speedMps) {
        return formatSpeed(new AndroidNavigationTextResources(context), speedMps);
    }

    @NonNull
    public static String formatSpeed(@NonNull NavigationTextResources resources, float speedMps) {
        if (!Float.isFinite(speedMps) || speedMps < 0f) {
            return resources.getString(R.string.nav_status_unavailable);
        }
        if (resources.isImperialUnitsEnabled()) {
            return resources.getString(R.string.format_nav_speed_mph_value,
                    speedMps * METERS_PER_SECOND_TO_MILES_PER_HOUR);
        }
        return resources.getString(R.string.format_nav_speed_value, speedMps * 3.6f);
    }

    @NonNull
    public static String formatElevation(@NonNull Context context, @Nullable Double elevationMeters) {
        return formatElevation(new AndroidNavigationTextResources(context), elevationMeters);
    }

    @NonNull
    public static String formatElevation(
            @NonNull NavigationTextResources resources,
            @Nullable Double elevationMeters
    ) {
        if (elevationMeters == null || !Double.isFinite(elevationMeters)) {
            return resources.getString(R.string.nav_status_unavailable);
        }
        if (resources.isImperialUnitsEnabled()) {
            return resources.getString(
                    R.string.format_nav_elevation_ft_value,
                    elevationMeters / METERS_PER_FOOT
            );
        }
        return resources.getString(R.string.format_nav_elevation_value, elevationMeters);
    }

    @NonNull
    public static String formatAccuracy(@NonNull Context context, float accuracyMeters) {
        return formatAccuracy(new AndroidNavigationTextResources(context), accuracyMeters);
    }

    @NonNull
    public static String formatAccuracy(@NonNull NavigationTextResources resources, float accuracyMeters) {
        if (!isDisplayableAccuracyMeters(accuracyMeters)) {
            return resources.getString(R.string.nav_status_unavailable);
        }
        if (resources.isImperialUnitsEnabled()) {
            return resources.getString(
                    R.string.format_nav_accuracy_ft_value,
                    accuracyMeters / METERS_PER_FOOT
            );
        }
        return resources.getString(R.string.format_nav_accuracy_value, accuracyMeters);
    }

    public static boolean isDisplayableAccuracyMeters(float accuracyMeters) {
        return Float.isFinite(accuracyMeters)
                && accuracyMeters > 0f
                && accuracyMeters <= MAX_DISPLAY_ACCURACY_METERS;
    }

    @NonNull
    private static String formatMetricDistance(@NonNull NavigationTextResources resources, double meters) {
        if (meters >= 1000.0) {
            return resources.getString(R.string.format_distance_km, meters / 1000.0);
        }
        return resources.getString(R.string.format_distance_m, meters);
    }

    @NonNull
    private static String formatImperialDistance(@NonNull NavigationTextResources resources, double meters) {
        double miles = meters / METERS_PER_MILE;
        if (miles >= MIN_MILES_DISPLAY_DISTANCE) {
            return resources.getString(R.string.format_distance_mi, miles);
        }
        return resources.getString(R.string.format_distance_ft, meters / METERS_PER_FOOT);
    }
}
