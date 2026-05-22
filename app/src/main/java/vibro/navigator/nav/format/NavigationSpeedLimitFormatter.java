package vibro.navigator.nav.format;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.nav.route.RouteSpeedLimit;

public final class NavigationSpeedLimitFormatter {
    private NavigationSpeedLimitFormatter() {
    }

    @NonNull
    public static String formatBadge(@NonNull RouteSpeedLimit speedLimit) {
        return String.valueOf(speedLimit.value);
    }

    @NonNull
    public static String formatContentDescription(
            @NonNull Context context,
            @NonNull RouteSpeedLimit speedLimit
    ) {
        return context.getString(
                R.string.format_speed_limit_content_description,
                formatWithUnit(context, speedLimit)
        );
    }

    @NonNull
    private static String formatWithUnit(@NonNull Context context, @NonNull RouteSpeedLimit speedLimit) {
        if (speedLimit.unit == RouteSpeedLimit.Unit.MILES_PER_HOUR) {
            return context.getString(R.string.format_speed_limit_mph, speedLimit.value);
        }
        return context.getString(R.string.format_speed_limit_kmh, speedLimit.value);
    }
}
