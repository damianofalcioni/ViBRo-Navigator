package vibro.navigator.nav.format;

import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import vibro.navigator.R;

public final class TestNavigationTextResources implements NavigationTextResources {
    private static final Map<Integer, String> STRINGS = buildStrings();
    private static final Map<Integer, PluralStrings> PLURALS = buildPlurals();

    private final boolean imperialUnitsEnabled;

    private TestNavigationTextResources(boolean imperialUnitsEnabled) {
        this.imperialUnitsEnabled = imperialUnitsEnabled;
    }

    @NonNull
    public static TestNavigationTextResources metric() {
        return new TestNavigationTextResources(false);
    }

    @NonNull
    public static TestNavigationTextResources imperial() {
        return new TestNavigationTextResources(true);
    }

    @NonNull
    @Override
    public String getString(@StringRes int resId, Object... formatArgs) {
        String pattern = STRINGS.get(resId);
        if (pattern == null) {
            throw new IllegalArgumentException("Unhandled test string resource: " + resId);
        }
        return formatArgs.length == 0 ? pattern : String.format(Locale.US, pattern, formatArgs);
    }

    @NonNull
    @Override
    public String getQuantityString(@PluralsRes int resId, int quantity, Object... formatArgs) {
        PluralStrings pluralStrings = PLURALS.get(resId);
        if (pluralStrings == null) {
            throw new IllegalArgumentException("Unhandled test plural resource: " + resId);
        }
        String pattern = pluralStrings.patternFor(quantity);
        return formatArgs.length == 0 ? pattern : String.format(Locale.US, pattern, formatArgs);
    }

    @Override
    public boolean isImperialUnitsEnabled() {
        return imperialUnitsEnabled;
    }

    @NonNull
    private static Map<Integer, String> buildStrings() {
        Map<Integer, String> strings = new HashMap<>();
        addDirectionStrings(strings);
        addMeasurementStrings(strings);
        addNavigationStatusStrings(strings);
        return strings;
    }

    private static void addDirectionStrings(@NonNull Map<Integer, String> strings) {
        strings.put(R.string.direction_continue, "Continue");
        strings.put(R.string.direction_keep_left, "Keep left");
        strings.put(R.string.direction_keep_right, "Keep right");
        strings.put(R.string.direction_turn_left, "Turn left");
        strings.put(R.string.direction_turn_right, "Turn right");
        strings.put(R.string.direction_slight_left, "Slight left");
        strings.put(R.string.direction_slight_right, "Slight right");
        strings.put(R.string.direction_sharp_left, "Sharp left");
        strings.put(R.string.direction_sharp_right, "Sharp right");
        strings.put(R.string.direction_uturn_left, "U-turn left");
        strings.put(R.string.direction_uturn, "U-turn");
        strings.put(R.string.direction_uturn_right, "U-turn right");
        strings.put(R.string.direction_roundabout_exit, "Roundabout, exit %1$d");
        strings.put(R.string.direction_roundabout_exit_left, "Roundabout, exit %1$d");
        strings.put(R.string.direction_beeline, "Beeline");
        strings.put(R.string.direction_exit_left, "Exit left");
        strings.put(R.string.direction_exit_right, "Exit right");
        strings.put(R.string.direction_offroute, "Off route");
        strings.put(R.string.direction_arrive, "Destination reached");
        strings.put(R.string.direction_intermediate_arrive, "Intermediate destination reached");
        strings.put(R.string.direction_unknown, "Unknown direction");
        strings.put(R.string.direction_side_left, "left");
        strings.put(R.string.direction_side_right, "right");
        strings.put(R.string.format_stop_label, "Stop %1$d");
    }

    private static void addMeasurementStrings(@NonNull Map<Integer, String> strings) {
        strings.put(R.string.format_distance_m, "%1$.0f m");
        strings.put(R.string.format_distance_km, "%1$.1f km");
        strings.put(R.string.format_distance_ft, "%1$.0f ft");
        strings.put(R.string.format_distance_mi, "%1$.1f mi");
        strings.put(R.string.format_bearing_degrees, "%1$.0f°");
        strings.put(R.string.format_time_s, "%1$d s");
        strings.put(R.string.format_time_min, "%1$d min");
        strings.put(R.string.format_duration_min_s, "%1$d min %2$02d s");
        strings.put(R.string.format_duration_h_min, "%1$d h %2$02d min");
        strings.put(R.string.format_nav_speed_value, "%1$.0f km/h");
        strings.put(R.string.format_nav_speed_mph_value, "%1$.0f mph");
        strings.put(R.string.format_nav_elevation_value, "%1$.0f m");
        strings.put(R.string.format_nav_elevation_ft_value, "%1$.0f ft");
        strings.put(R.string.format_nav_accuracy_value, "±%1$.0f m");
        strings.put(R.string.format_nav_accuracy_ft_value, "±%1$.0f ft");
        strings.put(R.string.format_nav_bearing_value, "%1$.0f°");
        strings.put(R.string.format_nav_bearing_accuracy_value, "%1$.0f°");
        strings.put(R.string.format_nav_satellite_count_value, "%1$d");
        strings.put(R.string.format_nav_fix_count_value, "#%1$d");
        strings.put(R.string.gpx_passed_route_track_name, "Passed route");
        strings.put(R.string.format_gpx_gps_fix_name, "GPS fix %1$d");
        strings.put(R.string.format_nav_gps_status, "%1$s ↑%2$s • %3$s • (%4$s)");
        strings.put(R.string.format_nav_gps_obtained_time_value, "%1$tH:%1$tM:%1$tS");
        strings.put(R.string.format_nav_gps_details,
                "Speed: %1$s\nAltitude: %2$s\nAccuracy: %3$s\nGPS obtained: %4$s\nSatellites: %5$s\n"
                        + "Interval: %6$s\nGPS fixes: %7$s\nGPS bearing: %8$s\nBearing accuracy: %9$s");
        strings.put(R.string.format_nav_battery_used_mah, "%1$.1f mAh");
        strings.put(R.string.format_nav_battery_drop_percent, "%1$d%%");
        strings.put(R.string.format_nav_trip_stats_details,
                "Elapsed: %1$s\nDistance: %2$s\nMoving time: %3$s\nStationary time: %4$s\n"
                        + "Average speed: %5$s\nMoving average: %6$s\nMax speed: %7$s\n"
                        + "Screen on: %8$s\nScreen off: %9$s\n"
                        + "Battery used: %10$s\nBattery drop: %11$s");
    }

    private static void addNavigationStatusStrings(@NonNull Map<Integer, String> strings) {
        strings.put(R.string.nav_destination_label, "🏁");
        strings.put(R.string.nav_eta, "ETA");
        strings.put(R.string.nav_status_unavailable, "--");
        strings.put(R.string.nav_no_route, "No route");
        strings.put(R.string.nav_waiting_for_location_title, "Waiting for location");
        strings.put(
                R.string.nav_waiting_for_location_body,
                "Waiting for a usable location fix to calculate the route."
        );
        strings.put(
                R.string.nav_waiting_for_location_straight_line_body,
                "Waiting for a usable location fix for straight-line guidance."
        );
        strings.put(R.string.nav_calculating_route_title, "Calculating route");
        strings.put(R.string.nav_calculating_route_body, "Getting directions from BRouter.");
        strings.put(R.string.nav_route_unavailable_title, "Route unavailable");
        strings.put(R.string.format_nav_route_unavailable_body,
                "Route unavailable: %1$s \n Check BRouter for available segments.");
        strings.put(R.string.nav_route_unavailable_generic, "unknown error");
        strings.put(R.string.nav_start_invalid_request, "navigation request is incomplete");
        strings.put(
                R.string.nav_route_notice_no_alternative_keep_current,
                "Could not find an alternative route around the blocked area. Keeping current route."
        );
        strings.put(R.string.nav_route_notice_no_route_found, "No route found for this destination.");
        strings.put(
                R.string.nav_route_notice_service_unavailable_keep_current,
                "Routing service unavailable. Keeping current route."
        );
        strings.put(R.string.nav_route_notice_service_unavailable, "Routing service unavailable.");
        strings.put(R.string.nav_route_notice_invalid_profile, "Routing profile is invalid.");
        strings.put(
                R.string.nav_route_notice_update_failed_keep_current,
                "Could not update the route. Keeping current route."
        );
        strings.put(R.string.nav_route_notice_unavailable, "Could not calculate a route.");
        strings.put(
                R.string.nav_route_notice_blocked_road_recalculating,
                "Blocked road added. Recalculating route."
        );
        strings.put(
                R.string.nav_paused_notice,
                "Navigation is paused. Tap Resume navigation to continue live guidance."
        );
        strings.put(R.string.format_progress_line, "%1$s: %2$s • %3$s • %4$s %5$s");
        strings.put(R.string.nav_destination_reached, "Destination reached");
        strings.put(R.string.notification_off_route_title, "Off route");
        strings.put(R.string.notification_wrong_direction_title, "Wrong direction");
        strings.put(R.string.format_turn_notification, "%1$s %2$s - %3$s - %4$s");
        strings.put(R.string.format_turn_speech, "%1$s, %2$s");
        strings.put(
                R.string.format_off_route_off_track_notification,
                "Off-track detected. Distance %1$s, threshold %2$s. Recalculating route."
        );
        strings.put(
                R.string.format_off_route_bearing_notification,
                "Bearing mismatch detected. Diff %1$s, expected %2$s, actual %3$s. Recalculating route."
        );
        strings.put(
                R.string.format_off_route_off_track_notice_only,
                "Off-track detected. Distance %1$s, threshold %2$s."
        );
        strings.put(
                R.string.format_off_route_bearing_notice_only,
                "Bearing mismatch detected. Diff %1$s, expected %2$s, actual %3$s."
        );
        strings.put(
                R.string.format_wrong_direction_notification,
                "Wrong direction detected. Target %1$s, actual %2$s, diff %3$s."
        );
        strings.put(
                R.string.format_startup_orientation_notification,
                "Turn yourself %1$s %2$s to face the route."
        );
    }

    @NonNull
    private static Map<Integer, PluralStrings> buildPlurals() {
        Map<Integer, PluralStrings> plurals = new HashMap<>();
        plurals.put(R.plurals.format_time_speech_seconds, new PluralStrings("%1$d second", "%1$d seconds"));
        plurals.put(R.plurals.format_time_speech_minutes, new PluralStrings("%1$d minute", "%1$d minutes"));
        return plurals;
    }

    private static final class PluralStrings {
        @NonNull
        private final String one;
        @NonNull
        private final String other;

        private PluralStrings(@NonNull String one, @NonNull String other) {
            this.one = one;
            this.other = other;
        }

        @NonNull
        private String patternFor(int quantity) {
            return quantity == 1 ? one : other;
        }
    }
}
