package vibro.navigator.nav.export;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;

final class NavigationRouteGpxFixWriter {
    private static final String TAG_WAYPOINT = "wpt";
    private static final String TYPE_GPS_FIX = "vibro.navigator.gps-fix";
    private static final String GPX_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private NavigationRouteGpxFixWriter() {
    }

    static void appendWaypoints(
            @NonNull StringBuilder out,
            @NonNull NavigationTextResources textResources,
            @NonNull List<NavigationLocation> acceptedFixes
    ) {
        for (int i = 0; i < acceptedFixes.size(); i++) {
            appendWaypoint(out, textResources, acceptedFixes.get(i), i + 1);
        }
    }

    private static void appendWaypoint(
            @NonNull StringBuilder out,
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationLocation location,
            int fixNumber
    ) {
        NavigationRouteGpxXmlWriter.appendPointStart(out, 1, TAG_WAYPOINT, locationPoint(location));
        out.append(">").append(NavigationRouteGpxXmlWriter.LINE_END);
        NavigationRouteGpxXmlWriter.appendSimpleElement(
                out,
                2,
                NavigationRouteGpxXmlWriter.TAG_NAME,
                textResources.getString(R.string.format_gpx_gps_fix_name, fixNumber)
        );
        appendTime(out, location);
        NavigationRouteGpxXmlWriter.appendSimpleElement(
                out,
                2,
                NavigationRouteGpxXmlWriter.TAG_TYPE,
                TYPE_GPS_FIX
        );
        out.append("  </wpt>").append(NavigationRouteGpxXmlWriter.LINE_END);
    }

    @NonNull
    private static LatLon locationPoint(@NonNull NavigationLocation location) {
        return new LatLon(location.getLatitude(), location.getLongitude());
    }

    private static void appendTime(@NonNull StringBuilder out, @NonNull NavigationLocation location) {
        String timestamp = formatTimestamp(location.getTime());
        if (timestamp != null) {
            NavigationRouteGpxXmlWriter.appendSimpleElement(
                    out,
                    2,
                    NavigationRouteGpxXmlWriter.TAG_TIME,
                    timestamp
            );
        }
    }

    @Nullable
    private static String formatTimestamp(long timeMs) {
        if (timeMs <= 0L) {
            return null;
        }
        SimpleDateFormat formatter = new SimpleDateFormat(GPX_TIME_PATTERN, Locale.US);
        formatter.setTimeZone(UTC);
        return formatter.format(new Date(timeMs));
    }
}
