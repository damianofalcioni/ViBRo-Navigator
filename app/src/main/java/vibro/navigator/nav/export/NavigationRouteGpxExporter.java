package vibro.navigator.nav.export;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import vibro.navigator.R;
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
        return export(textResources, route, intermediateStops, NavigationRouteGpxExportHistory.empty());
    }

    @NonNull
    public static String export(
            @NonNull NavigationTextResources textResources,
            @NonNull GeoJsonRoute route,
            @NonNull List<LatLon> intermediateStops,
            @NonNull NavigationRouteGpxExportHistory history
    ) {
        String resolvedRouteName = buildRouteName(new Date());
        StringBuilder out = new StringBuilder();
        NavigationRouteGpxXmlWriter.appendHeader(out);
        NavigationRouteGpxXmlWriter.appendMetadata(out, resolvedRouteName);
        appendPassedInstructionWaypoints(out, textResources, history);
        NavigationRouteGpxInstructionWriter.appendWaypoints(out, textResources, route);
        NavigationRouteGpxStopWriter.appendWaypoints(out, textResources, intermediateStops);
        NavigationRouteGpxFixWriter.appendWaypoints(out, textResources, history.acceptedFixes);
        NavigationRouteGpxXmlWriter.appendRoute(out, resolvedRouteName, route);
        NavigationRouteGpxXmlWriter.appendTrackSegments(
                out,
                textResources.getString(R.string.gpx_passed_route_track_name),
                passedSegments(history)
        );
        NavigationRouteGpxXmlWriter.appendTrack(out, resolvedRouteName, route);
        NavigationRouteGpxXmlWriter.appendFooter(out);
        return out.toString();
    }

    @NonNull
    public static String exportStraightLine(
            @NonNull NavigationTextResources textResources,
            @NonNull GeoJsonRoute route,
            @NonNull List<LatLon> intermediateStops,
            @NonNull LatLon destination
    ) {
        return exportStraightLine(
                textResources,
                route,
                intermediateStops,
                destination,
                NavigationRouteGpxExportHistory.empty()
        );
    }

    @NonNull
    public static String exportStraightLine(
            @NonNull NavigationTextResources textResources,
            @NonNull GeoJsonRoute route,
            @NonNull List<LatLon> intermediateStops,
            @NonNull LatLon destination,
            @NonNull NavigationRouteGpxExportHistory history
    ) {
        String resolvedRouteName = buildRouteName(new Date());
        StringBuilder out = new StringBuilder();
        NavigationRouteGpxXmlWriter.appendHeader(out);
        NavigationRouteGpxXmlWriter.appendMetadata(out, resolvedRouteName);
        NavigationRouteGpxStopWriter.appendWaypoints(out, textResources, intermediateStops);
        NavigationRouteGpxStopWriter.appendDestinationWaypoint(out, textResources, destination);
        NavigationRouteGpxFixWriter.appendWaypoints(out, textResources, history.acceptedFixes);
        NavigationRouteGpxXmlWriter.appendRoute(out, resolvedRouteName, route);
        NavigationRouteGpxXmlWriter.appendTrack(out, resolvedRouteName, route);
        NavigationRouteGpxXmlWriter.appendFooter(out);
        return out.toString();
    }

    private static void appendPassedInstructionWaypoints(
            @NonNull StringBuilder out,
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationRouteGpxExportHistory history
    ) {
        for (NavigationRouteGpxExportHistory.PassedRoute passedRoute : history.passedRoutes) {
            appendPassedInstructionWaypoints(out, textResources, passedRoute);
        }
    }

    private static void appendPassedInstructionWaypoints(
            @NonNull StringBuilder out,
            @NonNull NavigationTextResources textResources,
            @NonNull NavigationRouteGpxExportHistory.PassedRoute passedRoute
    ) {
        if (passedRoute.includeInstructionWaypoints) {
            NavigationRouteGpxInstructionWriter.appendPassedWaypoints(
                    out,
                    textResources,
                    passedRoute.route,
                    passedRoute.maxPassedTrackIndex
            );
        }
    }

    @NonNull
    private static List<List<LatLon>> passedSegments(@NonNull NavigationRouteGpxExportHistory history) {
        int segmentCount = history.passedRoutes.size() + history.recalculationBridgeSegments.size();
        List<List<LatLon>> segments = new ArrayList<>(segmentCount);
        for (int i = 0; i < history.passedRoutes.size(); i++) {
            NavigationRouteGpxExportHistory.PassedRoute passedRoute = history.passedRoutes.get(i);
            segments.add(passedRoute.segment);
            if (i < history.recalculationBridgeSegments.size()) {
                segments.add(history.recalculationBridgeSegments.get(i));
            }
        }
        appendRemainingBridgeSegments(history, segments);
        return segments;
    }

    private static void appendRemainingBridgeSegments(
            @NonNull NavigationRouteGpxExportHistory history,
            @NonNull List<List<LatLon>> segments
    ) {
        for (int i = history.passedRoutes.size(); i < history.recalculationBridgeSegments.size(); i++) {
            segments.add(history.recalculationBridgeSegments.get(i));
        }
    }

    @NonNull
    static String buildRouteName(@NonNull Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(ROUTE_NAME_DATE_PATTERN, Locale.US);
        return ROUTE_NAME_PREFIX + formatter.format(date);
    }
}
