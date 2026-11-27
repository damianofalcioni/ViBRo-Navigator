package vibro.navigator.nav.format;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;

public final class NavigationMeasurementFormatter {
    private static final double METERS_PER_SECOND_TO_MILES_PER_HOUR = 2.2369362921;
    private static final float MAX_DISPLAY_ACCURACY_METERS = 40_100_000f;

    private NavigationMeasurementFormatter() {
    }

    @NonNull
    public static String formatDistance(@NonNull Context context, double meters) {
        return formatDistance(new AndroidNavigationTextResources(context), meters);
    }

    @NonNull
    public static String formatDistance(@NonNull NavigationTextResources resources, double meters) {
        NavigationDistanceUnit unit = NavigationDistanceUnit.select(meters, resources.isImperialUnitsEnabled());
        return resources.getString(unit.formatResource, unit.fromMeters(meters));
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
                    NavigationDistanceUnit.FEET.fromMeters(elevationMeters)
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
                    NavigationDistanceUnit.FEET.fromMeters(accuracyMeters)
            );
        }
        return resources.getString(R.string.format_nav_accuracy_value, accuracyMeters);
    }

    @NonNull
    public static String formatAccuracyMeters(@NonNull NavigationTextResources resources, float accuracyMeters) {
        if (!isDisplayableAccuracyMeters(accuracyMeters)) {
            return resources.getString(R.string.nav_status_unavailable);
        }
        return resources.getString(R.string.format_nav_accuracy_value, accuracyMeters);
    }

    public static boolean isDisplayableAccuracyMeters(float accuracyMeters) {
        return Float.isFinite(accuracyMeters)
                && accuracyMeters > 0f
                && accuracyMeters <= MAX_DISPLAY_ACCURACY_METERS;
    }

}
