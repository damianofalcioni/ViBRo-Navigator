package vibro.navigator.nav.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassOrientationCue;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

public class NavigationBeelineOrientationCueTest {
    private static final LatLon ORIGIN = new LatLon(0, 0);
    private static final LatLon STOP = new LatLon(0.001, 0);
    private static final LatLon DESTINATION = new LatLon(0, 0.003);

    @Test
    public void command16CuePointsToIntermediateStopThenBackToRoadAndClearsOnCompletion() {
        NavigationSessionRouteState state = beelineState();
        NavigationLocation start = fix(ORIGIN, 1_000L);
        evaluate(state, start);
        assertTrue(state.isBeelineGuidanceActive());
        assertCue(compass(state, start, false, 270.0), 0f);

        NavigationLocation stop = fix(STOP, 4_000L);
        evaluate(state, stop);
        assertTrue(state.remainingIntermediateStops(Collections.emptyList()).isEmpty());
        assertTrue(state.isBeelineGuidanceActive());
        assertCue(compass(state, stop, false, 270.0), 180f);

        NavigationLocation road = fix(ORIGIN, 7_000L);
        evaluate(state, road);
        assertFalse(state.isBeelineGuidanceActive());
        NavCompassState completed = compass(state, road, false, 90.0);
        assertNull(completed.routeStartApproachProjection);
        assertNull(completed.orientationCue);
    }

    @Test
    public void beelineCueUpdatesWithPositionAndIgnoresStationaryRoadCueAndDisplayHeading() {
        NavigationSessionRouteState state = beelineState();
        evaluate(state, fix(ORIGIN, 1_000L));
        NavigationLocation moved = fix(new LatLon(0.0002, 0.0002), 4_000L);
        evaluate(state, moved);
        float targetBearing = (float) GeoMath.bearingDegrees(
                moved.getLatitude(), moved.getLongitude(), STOP.lat, STOP.lon);
        assertCue(compass(state, moved, false, 270.0), targetBearing);
        assertCue(compass(state, moved, true, 120.0), targetBearing);
        assertCue(compass(state, moved, true, 220.0), targetBearing);
    }

    @Test
    public void reachingRoadBeelineStartRadiusActivatesTargetBeforeProjectionPassesTheVertex() {
        LatLon roadStart = new LatLon(0, 0.001);
        LatLon intermediate = new LatLon(0.001, 0.001);
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(ORIGIN, roadStart, intermediate, roadStart, DESTINATION),
                Arrays.asList(new VoiceHint(1, 16, 0, 111, 0), new VoiceHint(2, 16, 0, 111, 0),
                        new VoiceHint(3, 5, 0, 222, -90)), 200, 556);
        NavigationSessionRouteState state = routeState(route, Collections.singletonList(intermediate));
        assertFalse(state.isBeelineGuidanceActive());
        NavigationLocation reached = fix(new LatLon(0, 0.00095), 4_000L);
        evaluate(state, reached);
        assertTrue(state.isBeelineGuidanceActive());
        assertCue(compass(state, reached, false, 90.0), (float) GeoMath.bearingDegrees(
                reached.getLatitude(), reached.getLongitude(), intermediate.lat, intermediate.lon));
    }

    @Test
    public void finishingRoadApproachActivatesOutgoingBeelineInTheSameLocationEvaluation() {
        LatLon roadStart = new LatLon(0, 0.001);
        LatLon intermediate = new LatLon(0.001, 0.001);
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(roadStart, intermediate, roadStart, DESTINATION),
                Arrays.asList(new VoiceHint(0, 16, 0, 111, 0), new VoiceHint(1, 16, 0, 111, 0),
                        new VoiceHint(2, 5, 0, 222, -90)), 200, 444);
        NavigationSessionRouteState state = routeState(route, Collections.singletonList(intermediate));
        assertCue(compass(state, fix(ORIGIN, 1_000L), false, 90.0), 90f);
        NavigationLocation reached = fix(roadStart, 4_000L);
        NavigationRouteEvaluation evaluation = state.evaluateLocation(
                reached, 1.4f, false, 5f, 90.0, 4_000L, 0L, false);
        assertTrue(state.isBeelineGuidanceActive());
        assertCue(compass(state, reached, false, 90.0), 0f);
        assertTrue(evaluation.turnEvents.stream().noneMatch(
                event -> event.hint != null && event.hint.command == 5));
    }

    @Test
    public void routeStartApproachCuePointsToSnappedStartInsteadOfTheRoadOrFinalDestination() {
        LatLon snappedStart = new LatLon(0, 0.001);
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(snappedStart, STOP, DESTINATION),
                Collections.emptyList(), 200, 500);
        NavigationSessionRouteState state = routeState(route, Collections.emptyList());
        assertTrue(state.isBeelineGuidanceActive());
        assertCue(compass(state, fix(ORIGIN, 1_000L), true, 180.0), 90f);
    }

    @Test
    public void enteringBeelineClearsCachedManeuverSoRoadCueIsResolvedFreshAfterwards() {
        CompassDisplayMemory memory = new CompassDisplayMemory();
        PolylineIndex index = new PolylineIndex(Arrays.asList(ORIGIN, new LatLon(0, 0.001)));
        assertNotNull(memory.resolveOrientationCue(null, 90, null, index, 0.0));
        assertEquals(0f, memory.resolveDirectGuidanceCue(fix(ORIGIN, 1_000L), STOP).targetHeadingDegrees, 0.01f);
        assertEquals(270f, memory.resolveOrientationCue(null, 90, null, index, 180.0)
                .targetHeadingDegrees, 0.01f);
    }

    private static NavigationSessionRouteState beelineState() {
        GeoJsonRoute route = new GeoJsonRoute(Arrays.asList(ORIGIN, STOP, ORIGIN, DESTINATION),
                Arrays.asList(new VoiceHint(0, 16, 0, 111, 0), new VoiceHint(1, 16, 0, 111, 0)),
                200, 556);
        return routeState(route, Collections.singletonList(STOP));
    }

    private static NavigationSessionRouteState routeState(GeoJsonRoute route, List<LatLon> stops) {
        NavigationSessionRouteState state = new NavigationSessionRouteState();
        NavigationRouteRequestSnapshot request = new NavigationRouteRequestSnapshot(1, 1,
                NavigationRoutingMode.BROUTER, ORIGIN, stops, DESTINATION, "trekking", null,
                Collections.emptyList(), 0);
        state.applyRouteResult(TestNavigationTextResources.metric(), request, route,
                fix(ORIGIN, 1_000L), 1.4f, false, 1_000L);
        return state;
    }

    private static void evaluate(NavigationSessionRouteState state, NavigationLocation location) {
        state.evaluateLocation(location, 1.4f, false, 5f, 270.0, location.getTime(), 0L, false);
    }

    private static NavCompassState compass(NavigationSessionRouteState state, NavigationLocation location,
            boolean stationary, double heading) {
        NavState snapshot = state.advanceDisplayState(NavigationDisplaySnapshot.builder(TestNavigationTextResources.metric())
                .location(location, 1.4f, stationary, 5f)
                .heading(heading, 5f)
                .orientationCue(stationary ? new CompassOrientationCue(270f) : null)
                .timing(NavState.NO_DEADLINE, location.getTime())
                .build());
        assertNotNull(snapshot.routeStatus.compassState);
        return snapshot.routeStatus.compassState;
    }

    private static void assertCue(NavCompassState state, float heading) {
        assertNotNull(state.orientationCue);
        assertEquals(heading, state.orientationCue.targetHeadingDegrees, 0.01f);
    }

    private static NavigationLocation fix(LatLon point, long nowMs) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(point.lat);
        location.setLongitude(point.lon);
        location.setAccuracy(5f);
        location.setTime(nowMs, nowMs);
        return location;
    }
}
