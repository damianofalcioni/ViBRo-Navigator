package vibro.navigator.nav.session;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassPassedRouteSegments;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import static org.junit.Assert.*;

public class DirectLegTravelHistoryTest {
    private static final double COORDINATE_TOLERANCE = 0.0000001;
    private static final String ROUTING_PROFILE = "shortest";

    @Test
    public void unfinishedNativeBeelineKeepsFixesThenPlansFromItsTarget() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(new LatLon(0, 0), new LatLon(0, 0.001),
                new LatLon(0, 0.003)), Collections.singletonList(new VoiceHint(0, 16, 0, 111, 0)), 60, 333);
        NavigationRouteRequestSnapshot request = new NavigationRouteRequestSnapshot(1, 1, new LatLon(0, 0),
                Collections.emptyList(), new LatLon(0, 0.003), "trekking", null, Collections.emptyList());
        state.applyRouteResult(TestNavigationTextResources.metric(), request, route, fix(0, 0, 1_000), 3, 500);
        accept(state, fix(0.0003, 0.0003, 4_000));
        accept(state, fix(0.0004, 0.0006, 7_000));
        NavigationRouteHistory history = state.historyForExport();
        List<LatLon> connector = history.recalculationBridgeSegmentsSnapshot().get(0);
        assertEquals(3, connector.size());
        assertEquals(0.0006, connector.get(2).lon, 0.0000001);
        assertEquals(0.001, history.remainingRoute().track.get(0).lon, 0.0000001);
        assertTrue(history.entryMeters() > 100);
        accept(state, fix(0, 0.001, 10_000));
        assertEquals(1, history.recalculationBridgeSegmentsSnapshot().size());
        assertEquals(0.001, history.remainingRoute().track.get(0).lon, 0.0000001);
    }

    @Test
    public void intermediateOutAndBackBeelinesKeepOneChronologicalFixPath() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        LatLon routePoint = new LatLon(0, 0.001);
        LatLon stop = new LatLon(0.0001, 0.001);
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0, 0),
                        routePoint,
                        stop,
                        routePoint,
                        new LatLon(0, 0.002)
                ),
                Arrays.asList(
                        new VoiceHint(1, 16, 0, 11, 0),
                        new VoiceHint(2, 16, 0, 11, 0)
                ),
                60,
                244
        );
        NavigationRouteRequestSnapshot request = new NavigationRouteRequestSnapshot(
                1,
                1,
                new LatLon(0, 0),
                Collections.singletonList(stop),
                new LatLon(0, 0.002),
                "trekking",
                null,
                Collections.emptyList()
        );
        state.applyRouteResult(
                TestNavigationTextResources.metric(),
                request,
                route,
                fix(0, 0, 1_000),
                3,
                500
        );

        accept(state, fix(0, 0.001, 4_000, 1));
        LatLon reachedFix = new LatLon(0.00005, 0.001);
        accept(state, fix(reachedFix.lat, reachedFix.lon, 7_000, 1));
        accept(state, fix(0, 0.001, 10_000, 1));

        List<List<LatLon>> connectors =
                state.historyForExport().recalculationBridgeSegmentsSnapshot();
        assertEquals(1, connectors.size());
        assertEquals(1, occurrenceCount(connectors.get(0), reachedFix));
        assertEquals(0, occurrenceCount(connectors.get(0), stop));
    }

    @Test
    public void accuracyReachedIntermediateSpurIsNotArchivedAsTravelled() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        LatLon roadStart = new LatLon(48.18053793934306, 16.377720942747157);
        LatLon spurStart = new LatLon(48.180628, 16.378131);
        LatLon stop = new LatLon(48.180668, 16.378115);
        LatLon roadAfter = new LatLon(48.180690, 16.378485);
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(roadStart, spurStart, stop, spurStart, roadAfter),
                Arrays.asList(
                        new VoiceHint(1, 16, 0, 5, 0),
                        new VoiceHint(2, 16, 0, 5, 0)
                ),
                60,
                100
        );
        NavigationRouteRequestSnapshot request = new NavigationRouteRequestSnapshot(
                1,
                1,
                roadStart,
                Collections.singletonList(stop),
                roadAfter,
                ROUTING_PROFILE,
                null,
                Collections.emptyList()
        );
        state.applyRouteResult(
                TestNavigationTextResources.metric(),
                request,
                route,
                fix(roadStart.lat, roadStart.lon, 1_000, 6),
                3,
                500
        );

        NavigationLocation fix15 =
                fix(48.18057449299612, 16.37790535953947, 4_000, 6.500651f);
        NavigationLocation fix16 =
                fix(48.180594250870136, 16.378019690039558, 7_000, 4.8816f);
        NavigationLocation fix17 =
                fix(48.18061522301826, 16.378128775653604, 10_000, 3.906723f);
        NavigationLocation fix18 =
                fix(48.18063501066919, 16.378230734925204, 13_000, 6.328909f);
        accept(state, fix15);
        accept(state, fix16);
        accept(state, fix17);
        accept(state, fix18);

        NavigationRouteHistory history = state.historyForExport();
        assertTrue(history.recalculationBridgeSegmentsSnapshot().isEmpty());
        for (List<LatLon> section : history.orderedSegmentsSnapshot()) {
            assertEquals(0, occurrenceCount(section, stop));
        }
        assertTrue(history.remainingRoute().track.get(0).lon > spurStart.lon);

        String gpx = NavigationSessionRouteExporter.export(
                TestNavigationTextResources.metric(),
                state,
                new StraightLineNavigationState(),
                fix18,
                Arrays.asList(fix15, fix16, fix17, fix18),
                new NavigationRequest(
                        ROUTING_PROFILE,
                        "Destination",
                        roadAfter,
                        Collections.singletonList(stop)
                )
        );
        assertNotNull(gpx);
        assertTrue(gpx.contains("<name>Stop 1</name>"));
        String passedTrack = firstTrack(gpx);
        assertFalse(passedTrack.contains("<trkpt lat=\"48.180668\" lon=\"16.378115\""));
        assertFalse(passedTrack.contains("<trkpt lat=\"48.180574\" lon=\"16.377905\""));
        assertFalse(passedTrack.contains("<trkpt lat=\"48.180594\" lon=\"16.378020\""));
        assertFalse(passedTrack.contains("<trkpt lat=\"48.180615\" lon=\"16.378129\""));

    }

    @Test
    public void forwardProjectionNearSpurStartDoesNotArchiveSpurAsRoad() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        LatLon roadStart = new LatLon(0, 0);
        LatLon spurStart = new LatLon(0, 0.001);
        LatLon stop = new LatLon(0.0004, 0.001);
        LatLon forwardProjection = new LatLon(0.00005, 0.00102);
        LatLon destination = new LatLon(0.00005, 0.002);
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        roadStart,
                        spurStart,
                        stop,
                        spurStart,
                        forwardProjection,
                        destination
                ),
                Arrays.asList(
                        new VoiceHint(1, 16, 0, 44, 0),
                        new VoiceHint(2, 16, 0, 44, 0)
                ),
                120,
                300
        );
        NavigationRequest request = new NavigationRequest(
                ROUTING_PROFILE,
                "Destination",
                destination,
                Collections.singletonList(stop)
        );
        state.applyRouteResult(
                TestNavigationTextResources.metric(),
                new NavigationRouteRequestSnapshot(
                        1,
                        1,
                        roadStart,
                        Collections.singletonList(stop),
                        destination,
                        ROUTING_PROFILE,
                        null,
                        Collections.emptyList()
                ),
                route,
                fix(roadStart.lat, roadStart.lon, 1_000, 1),
                3,
                500
        );

        NavigationLocation projectedFix = fix(
                forwardProjection.lat,
                forwardProjection.lon,
                4_000,
                1
        );
        accept(state, projectedFix);
        state.applyRouteResult(
                TestNavigationTextResources.metric(),
                new NavigationRouteRequestSnapshot(
                        2,
                        2,
                        forwardProjection,
                        Collections.emptyList(),
                        destination,
                        ROUTING_PROFILE,
                        null,
                        Collections.emptyList()
                ),
                new GeoJsonRoute(
                        Arrays.asList(forwardProjection, destination),
                        Collections.emptyList(),
                        60,
                        100
                ),
                projectedFix,
                3,
                4_500
        );

        NavigationRouteHistory history = state.historyForExport();
        for (List<LatLon> section : history.archivedSegmentsSnapshot()) {
            assertEquals(0, occurrenceCount(section, stop));
        }

        NavState navState = state.buildState(
                TestNavigationTextResources.metric(),
                projectedFix,
                3,
                false,
                1,
                null,
                null,
                null,
                NavState.NO_DEADLINE,
                5_000,
                false,
                null,
                null
        );
        assertNotNull(navState.routeStatus.compassState);
        assertEquals(
                0,
                occurrenceCount(
                        navState.routeStatus.compassState.archivedPassedRouteSegments(),
                        stop
                )
        );

        String gpx = NavigationSessionRouteExporter.export(
                TestNavigationTextResources.metric(),
                state,
                new StraightLineNavigationState(),
                projectedFix,
                Collections.singletonList(projectedFix),
                request
        );
        assertNotNull(gpx);
        String stopPoint = "<trkpt lat=\"0.000400\" lon=\"0.001000\"";
        assertFalse(firstTrack(gpx).contains(stopPoint));
        assertFalse(routeElement(gpx).contains("<rtept lat=\"0.000400\" lon=\"0.001000\""));
    }

    @Test
    public void returnConnectorStartsAtFixThatAccuracyReachedOutboundLeg() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        LatLon routePoint = new LatLon(0, 0.001);
        LatLon stop = new LatLon(0.0002, 0.001);
        LatLon destination = new LatLon(0, 0.002);
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(new LatLon(0, 0), routePoint, stop, routePoint, destination),
                Arrays.asList(
                        new VoiceHint(1, 16, 0, 22, 0),
                        new VoiceHint(2, 16, 0, 22, 0)
                ),
                60,
                266
        );
        NavigationRouteRequestSnapshot request = new NavigationRouteRequestSnapshot(
                1,
                1,
                new LatLon(0, 0),
                Collections.singletonList(stop),
                destination,
                ROUTING_PROFILE,
                null,
                Collections.emptyList()
        );
        state.applyRouteResult(
                TestNavigationTextResources.metric(),
                request,
                route,
                fix(0, 0, 1_000, 1),
                3,
                500
        );

        accept(state, fix(0, 0.0008, 4_000, 1));
        LatLon accuracyReachedFix = new LatLon(0.00012, 0.001);
        accept(state, fix(accuracyReachedFix.lat, accuracyReachedFix.lon, 7_000, 1));
        accept(state, fix(routePoint.lat, routePoint.lon, 10_000, 1));

        List<List<LatLon>> connectors =
                state.historyForExport().recalculationBridgeSegmentsSnapshot();
        assertEquals(1, connectors.size());
        assertEquals(2, connectors.get(0).size());
        assertPoint(accuracyReachedFix, connectors.get(0).get(0));
        assertPoint(routePoint, connectors.get(0).get(1));
    }

    @Test
    public void finalBeelineDoesNotReappearAfterArrivalBacktrackingFix() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        LatLon destination = new LatLon(0, 0.0011);
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(new LatLon(0, 0), new LatLon(0, 0.001), destination),
                Collections.singletonList(new VoiceHint(1, 16, 0, 11, 0)),
                60,
                122
        );
        NavigationRouteRequestSnapshot request = new NavigationRouteRequestSnapshot(
                1,
                1,
                new LatLon(0, 0),
                Collections.emptyList(),
                destination,
                "trekking",
                null,
                Collections.emptyList()
        );
        state.applyRouteResult(
                TestNavigationTextResources.metric(),
                request,
                route,
                fix(0, 0, 1_000),
                3,
                500
        );

        accept(state, fix(0, 0.001, 4_000, 1));
        accept(state, fix(destination.lat, destination.lon, 7_000, 1));
        accept(state, fix(0, 0.00105, 10_000, 1));

        List<LatLon> remaining = state.historyForExport().remainingRoute().track;
        assertEquals(1, remaining.size());
        assertPoint(destination, remaining.get(0));
    }

    @Test
    public void accuracyReachedFinalBeelineIsNotArchivedAsTravelled() {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        LatLon roadStart = new LatLon(48.179655, 16.378698);
        LatLon beelineStart = new LatLon(48.179510, 16.378727);
        LatLon destination = new LatLon(48.179498, 16.378590);
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(roadStart, beelineStart, destination),
                Collections.singletonList(new VoiceHint(1, 16, 0, 10, 0)),
                60,
                30
        );
        NavigationRouteRequestSnapshot request = new NavigationRouteRequestSnapshot(
                1,
                1,
                roadStart,
                Collections.emptyList(),
                destination,
                ROUTING_PROFILE,
                null,
                Collections.emptyList()
        );
        state.applyRouteResult(
                TestNavigationTextResources.metric(),
                request,
                route,
                fix(roadStart.lat, roadStart.lon, 1_000, 4.1f),
                3,
                500
        );

        accept(state, fix(48.17969414224094, 16.378675793129283, 4_000, 4.11141f));
        accept(state, fix(48.17956715986692, 16.37870524187438, 7_000, 5.038142f));
        accept(state, fix(48.179480281783285, 16.37872236670576, 10_000, 6.8121023f));

        NavigationRouteHistory history = state.historyForExport();
        assertTrue(history.recalculationBridgeSegmentsSnapshot().isEmpty());
        for (List<LatLon> section : history.orderedSegmentsSnapshot()) {
            assertEquals(0, occurrenceCount(section, destination));
        }
        assertEquals(1, history.remainingRoute().track.size());
        assertPoint(destination, history.remainingRoute().track.get(0));
    }

    private static void accept(NavigationSessionRouteState state, NavigationLocation fix) {
        NavigationRouteEvaluation evaluation = state.evaluateLocation(
                fix,
                3,
                fix.getAccuracy(),
                90.0,
                fix.getTime(),
                0
        );
        assertFalse(evaluation.shouldRecalculateRoute());
        state.recordRecalculationFixPath(fix, evaluation, false);
    }

    private static NavigationLocation fix(double lat, double lon, long time) {
        return fix(lat, lon, time, 10);
    }

    private static NavigationLocation fix(double lat, double lon, long time, float accuracy) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(time);
        location.setAccuracy(accuracy);
        return location;
    }

    private static int occurrenceCount(List<LatLon> points, LatLon expected) {
        int count = 0;
        for (LatLon point : points) {
            if (Math.abs(point.lat - expected.lat) <= COORDINATE_TOLERANCE
                    && Math.abs(point.lon - expected.lon) <= COORDINATE_TOLERANCE) {
                count++;
            }
        }
        return count;
    }

    private static int occurrenceCount(
            CompassPassedRouteSegments segments,
            LatLon expected
    ) {
        int count = 0;
        for (int segmentIndex = 0; segmentIndex < segments.segmentCount(); segmentIndex++) {
            for (int pointIndex = 0;
                    pointIndex < segments.samplePointCount(segmentIndex);
                    pointIndex++) {
                LatLon point = segments.samplePointAt(segmentIndex, pointIndex);
                if (point != null
                        && Math.abs(point.lat - expected.lat) <= COORDINATE_TOLERANCE
                        && Math.abs(point.lon - expected.lon) <= COORDINATE_TOLERANCE) {
                    count++;
                }
            }
        }
        return count;
    }

    private static String firstTrack(String gpx) {
        int start = gpx.indexOf("<trk>");
        int end = gpx.indexOf("</trk>", start);
        return gpx.substring(start, end);
    }

    private static String routeElement(String gpx) {
        int start = gpx.indexOf("<rte>");
        int end = gpx.indexOf("</rte>", start);
        return gpx.substring(start, end);
    }

    private static void assertPoint(LatLon expected, LatLon actual) {
        assertEquals(expected.lat, actual.lat, COORDINATE_TOLERANCE);
        assertEquals(expected.lon, actual.lon, COORDINATE_TOLERANCE);
    }
}
