package vibro.navigator.nav.session;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.export.NavigationRouteGpxExportHistory;
import vibro.navigator.nav.export.NavigationRouteGpxExporter;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;
import static org.junit.Assert.*;

public class NavigationTravelHistoryRegressionTest {
    @Test
    public void connectorIncludesDepartureEveryFixAndRejoinWithoutBypassedRoutePrefix() {
        NavigationRouteHistory history = new NavigationRouteHistory();
        GeoJsonRoute first = route(0, 0, 0.001, 0.002);
        PolylineIndex firstIndex = new PolylineIndex(first.track);
        history.onRouteApplied(first, firstIndex, null, false);
        accept(history, firstIndex, fix(0, 0.0008, 1_000), stable());
        accept(history, firstIndex, fix(0.0002, 0.0008, 4_000), tentative());
        GeoJsonRoute second = route(0.001, 0, 0.002, 0.003);
        PolylineIndex secondIndex = new PolylineIndex(second.track);
        history.onRouteApplied(second, secondIndex, history.lastActiveRouteMatch(), false);
        history.recordRerouteFixPath(fix(0.0005, 0.001, 7_000), waiting(), false);

        List<LatLon> live = history.recalculationBridgeSegmentsSnapshot().get(0);
        assertEquals(3, live.size());
        assertPoint(live.get(0), 0, 0.0008);
        assertPoint(live.get(2), 0.0005, 0.001);

        accept(history, secondIndex, fix(0.00101, 0.002, 10_000), stable());
        accept(history, secondIndex, fix(0.001, 0.0025, 13_000), stable());
        List<List<LatLon>> sections = history.orderedSegmentsSnapshot();
        assertEquals(3, sections.size());
        assertPoint(sections.get(1).get(0), 0, 0.0008);
        List<LatLon> connector = sections.get(1);
        assertPoint(connector.get(connector.size() - 1), 0.001, 0.002);
        assertPoint(sections.get(2).get(0), 0.001, 0.002);
        assertPoint(history.remainingRoute().track.get(0), 0.001, 0.0025);
        String gpx = export(history);
        assertFalse(gpx.contains("lat=\"0.001000\" lon=\"0.000000\""));
        String journey = gpx.substring(gpx.indexOf("<rte>"), gpx.indexOf("</rte>"));
        assertTrue(journey.indexOf("lat=\"0.000200\"") < journey.indexOf("lat=\"0.001000\""));
    }

    @Test
    public void tentativeCornerExcursionDisappearsWhenOriginalRoadIsReacquired() {
        NavigationRouteHistory history = new NavigationRouteHistory();
        GeoJsonRoute route = route(0, 0, 0.001, 0.002);
        PolylineIndex index = new PolylineIndex(route.track);
        history.onRouteApplied(route, index, null, false);
        accept(history, index, fix(0, 0.0008, 1_000), stable());
        accept(history, index, fix(0.0002, 0.0009, 2_000), tentative());
        accept(history, index, fix(0, 0.0011, 5_000), stable());
        assertTrue(history.recalculationBridgeSegmentsSnapshot().isEmpty());
        assertEquals(1, history.orderedSegmentsSnapshot().size());
        assertFalse(export(history).contains("lat=\"0.000200\""));
    }

    @Test
    public void existingTurnJustBeyondDepartureIsPreservedButNextUnreachedTurnIsNot() {
        NavigationRouteHistory history = new NavigationRouteHistory();
        GeoJsonRoute route = route(0, 0, 0.001, 0.002);
        PolylineIndex index = new PolylineIndex(route.track);
        history.onRouteApplied(route, index, null, false);
        accept(history, index, fix(0, 0.00095, 1_000), stable());
        accept(history, index, fix(0.00008, 0.001, 4_000), tentative());
        GeoJsonRoute replacement = route(0.001, 0.001, 0.002, 0.003);
        history.onRouteApplied(replacement, new PolylineIndex(replacement.track), history.lastActiveRouteMatch(), false);
        String gpx = export(history);
        assertTrue(gpx.contains("lat=\"0.000000\" lon=\"0.001000\">\n    <name>Turn left"));
        assertFalse(gpx.contains("lat=\"0.000000\" lon=\"0.002000\">\n    <name>Turn right"));
    }

    @Test
    public void compassUsesTheSameLiveConnectorAndTrimsJoinedRoute() {
        NavigationRouteHistory history = new NavigationRouteHistory();
        GeoJsonRoute route = route(0, 0, 0.001, 0.002);
        PolylineIndex index = new PolylineIndex(route.track);
        history.startApproach(fix(0.001, 0, 1_000));
        history.onRouteApplied(route, index, null, false);
        history.recordRerouteFixPath(fix(0.0005, 0.001, 4_000), waiting(), false);
        CompassDisplayMemory compass = new CompassDisplayMemory();
        compass.onRouteApplied(route, index, Collections.emptyList());
        synchronize(compass, history);
        assertEquals(1, compass.routeGeometry().recalculationBridgeSegments().segmentCount());
        accept(history, index, fix(0, 0.0015, 7_000), stable());
        synchronize(compass, history);
        assertPoint(compass.routeGeometry().fullRoutePointAt(0), 0, 0.0015);
    }

    @Test
    public void queuedRecalculationNeverArchivesAnIntermediateRouteAsTravelled() {
        NavigationRouteHistory history = new NavigationRouteHistory();
        GeoJsonRoute original = route(0, 0, 0.001, 0.002);
        PolylineIndex originalIndex = new PolylineIndex(original.track);
        history.onRouteApplied(original, originalIndex, null, false);
        accept(history, originalIndex, fix(0, 0.0008, 1_000), stable());
        accept(history, originalIndex, fix(0.0002, 0.0008, 4_000), tentative());
        GeoJsonRoute intermediate = route(0.0005, 0, 0.001, 0.002);
        PolylineIndex intermediateIndex = new PolylineIndex(intermediate.track);
        history.onRouteApplied(intermediate, intermediateIndex, history.lastActiveRouteMatch(), false);
        NavigationLocation interimFix = fix(0.0005, 0.001, 7_000);
        history.recordProgress(intermediateIndex.match(new LatLon(0.0005, 0.001), -1));
        history.recordRerouteFixPath(interimFix, stable(), true);
        assertTrue(history.hasPendingRerouteFixPath());
        GeoJsonRoute finalRoute = route(0.001, 0, 0.002, 0.003);
        PolylineIndex finalIndex = new PolylineIndex(finalRoute.track);
        history.onRouteApplied(finalRoute, finalIndex, history.lastActiveRouteMatch(), false);
        accept(history, finalIndex, fix(0.001, 0.002, 10_000), stable());
        String gpx = export(history);
        assertFalse(gpx.contains("lat=\"0.000500\" lon=\"0.000000\""));
        assertFalse(gpx.contains("lat=\"0.001000\" lon=\"0.000000\""));
        assertTrue(gpx.contains("lat=\"0.000500\" lon=\"0.001000\""));
    }

    private static void accept(NavigationRouteHistory history, PolylineIndex index,
            NavigationLocation fix, NavigationRouteEvaluation evaluation) {
        if (evaluation.isStableOnRouteSample()) {
            history.recordProgress(index.match(new LatLon(fix.getLatitude(), fix.getLongitude()), -1));
        }
        history.recordRerouteFixPath(fix, evaluation, false);
    }

    private static String export(NavigationRouteHistory history) {
        return NavigationRouteGpxExporter.export(TestNavigationTextResources.metric(), history.remainingRoute(),
                Collections.emptyList(), new NavigationRouteGpxExportHistory(history.passedRoutesSnapshot(),
                        history.recalculationBridgeSegmentsSnapshot(), Collections.emptyList(),
                        history.orderedSegmentsSnapshot()));
    }

    private static void synchronize(CompassDisplayMemory compass, NavigationRouteHistory history) {
        compass.synchronizeHistory(history.entryMeters(), history.archivedSegmentsSnapshot(),
                history.recalculationBridgeSegmentsSnapshot());
    }

    private static GeoJsonRoute route(double lat, double start, double middle, double end) {
        return new GeoJsonRoute(Arrays.asList(new LatLon(lat, start), new LatLon(lat, middle), new LatLon(lat, end)),
                Arrays.asList(new VoiceHint(1, 2, 0, 100, -90), new VoiceHint(2, 5, 0, 0, 90)), 60, 300);
    }

    private static NavigationLocation fix(double lat, double lon, long time) {
        NavigationLocation fix = new NavigationLocation("gps");
        fix.setLatitude(lat);
        fix.setLongitude(lon);
        fix.setTime(time);
        fix.setAccuracy(10);
        return fix;
    }

    private static NavigationRouteEvaluation stable() {
        return NavigationRouteEvaluation.keepRoute(Collections.emptyList(), 3_000, true);
    }

    private static NavigationRouteEvaluation tentative() {
        return NavigationRouteEvaluation.waitForDeviationConfirmation(RouteDeviationPolicy.Reason.OFF_TRACK);
    }

    private static NavigationRouteEvaluation waiting() {
        return NavigationRouteEvaluation.keepRoute(Collections.emptyList(), 3_000, false);
    }

    private static void assertPoint(LatLon point, double lat, double lon) {
        assertEquals(lat, point.lat, 0.0000001);
        assertEquals(lon, point.lon, 0.0000001);
    }
}
