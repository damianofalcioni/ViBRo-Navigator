package vibro.navigator.nav.export;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.route.GeoJsonRoute;

public final class NavigationRouteGpxExporter {
    public static final String GPX_MIME_TYPE = "application/gpx+xml";

    private static final String ROUTE_NAME_PREFIX = "ViBRo-Navigator Export ";
    private static final String ROUTE_NAME_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private NavigationRouteGpxExporter() {
    }

    @NonNull
    public static String export(
            @NonNull NavigationTextResources textResources,
            @NonNull GeoJsonRoute route,
            @NonNull List<LatLon> intermediateStops
    ) {
        String resolvedRouteName = buildRouteName(new Date());
        StringBuilder out = new StringBuilder();
        NavigationRouteGpxXmlWriter.appendHeader(out);
        NavigationRouteGpxXmlWriter.appendMetadata(out, resolvedRouteName);
        NavigationRouteGpxInstructionWriter.appendWaypoints(out, textResources, route);
        NavigationRouteGpxStopWriter.appendWaypoints(out, textResources, intermediateStops);
        NavigationRouteGpxXmlWriter.appendRoute(out, resolvedRouteName, route);
        NavigationRouteGpxXmlWriter.appendTrack(out, resolvedRouteName, route);
        NavigationRouteGpxXmlWriter.appendFooter(out);
        return out.toString();
    }

    @NonNull
    static String buildRouteName(@NonNull Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(ROUTE_NAME_DATE_PATTERN, Locale.US);
        return ROUTE_NAME_PREFIX + formatter.format(date);
    }
}
